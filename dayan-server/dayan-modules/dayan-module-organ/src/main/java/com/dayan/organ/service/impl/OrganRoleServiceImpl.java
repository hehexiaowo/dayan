package com.dayan.organ.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dayan.common.core.code.CodeGenerator;
import com.dayan.common.core.exception.BusinessException;
import com.dayan.common.core.exception.ErrorCode;
import com.dayan.common.core.resp.PageResult;
import com.dayan.organ.dto.OrganRoleCreateDTO;
import com.dayan.organ.dto.OrganRoleQueryDTO;
import com.dayan.organ.dto.OrganRoleUpdateDTO;
import com.dayan.organ.entity.OrganAccountRoleRel;
import com.dayan.organ.entity.OrganRole;
import com.dayan.organ.entity.OrganRolePermissionShip;
import com.dayan.organ.mapper.OrganAccountRoleRelMapper;
import com.dayan.organ.mapper.OrganRoleMapper;
import com.dayan.organ.mapper.OrganRolePermissionShipMapper;
import com.dayan.organ.service.OrganRoleService;
import com.dayan.organ.vo.OrganRoleSimpleVO;
import com.dayan.organ.vo.OrganRoleVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * organ 域角色管理服务实现。
 *
 * <p>角色编码：{@code RL + 5位序列}（如 RL00001），全局唯一；同一 organCode 下不允许重名。
 * <p>注意：{@code OR} 前缀已被组织编码（{@link com.dayan.common.core.code.BusinessCode#ORGAN}）占用，
 * 角色独立使用 {@code RL}（RoLe）前缀，避免 CodeGenerator 序列键冲突。
 * 删除前校验是否有账号关联，授权采用"先删后增"全量覆盖语义。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrganRoleServiceImpl implements OrganRoleService {

    /** 角色编码前缀（RoLe） */
    private static final String ROLE_CODE_PREFIX = "RL";

    private final OrganRoleMapper roleMapper;
    private final OrganRolePermissionShipMapper rolePermissionShipMapper;
    private final OrganAccountRoleRelMapper accountRoleRelMapper;
    private final CodeGenerator codeGenerator;

    @Override
    public PageResult<OrganRoleSimpleVO> page(OrganRoleQueryDTO query) {
        LambdaQueryWrapper<OrganRole> wrapper = new LambdaQueryWrapper<OrganRole>()
                .eq(query.getOrganCode() != null && !query.getOrganCode().isEmpty(),
                        OrganRole::getOrganCode, query.getOrganCode())
                .like(query.getRoleName() != null && !query.getRoleName().isEmpty(),
                        OrganRole::getRoleName, query.getRoleName())
                .eq(query.getStatus() != null, OrganRole::getStatus, query.getStatus())
                .orderByAsc(OrganRole::getSortOrder)
                .orderByAsc(OrganRole::getId);

        Page<OrganRole> page = new Page<>(
                query.getCurrent() == null ? 1L : query.getCurrent(),
                query.getSize() == null ? 10L : query.getSize());
        IPage<OrganRole> result = roleMapper.selectPage(page, wrapper);

        List<OrganRoleSimpleVO> records = result.getRecords().stream()
                .map(this::toSimpleVO)
                .collect(Collectors.toList());
        return new PageResult<>(result.getCurrent(), result.getSize(), result.getTotal(), records);
    }

    @Override
    public OrganRoleVO getDetail(String roleCode) {
        OrganRole role = requireRole(roleCode);
        OrganRoleVO vo = toVO(role);
        vo.setPermissionCodes(listPermissions(roleCode));
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String create(OrganRoleCreateDTO dto) {
        // 1. 生成 roleCode
        String roleCode = codeGenerator.generate(ROLE_CODE_PREFIX);

        // 2. 校验 organCode + roleCode 唯一（roleCode 全局唯一）
        Long exist = roleMapper.selectCount(new LambdaQueryWrapper<OrganRole>()
                .eq(OrganRole::getRoleCode, roleCode));
        if (exist != null && exist > 0) {
            throw new BusinessException(ErrorCode.BUSINESS, "角色编码已存在：" + roleCode);
        }

        // 3. 落库
        OrganRole role = new OrganRole();
        role.setOrganCode(dto.getOrganCode());
        role.setRoleCode(roleCode);
        role.setRoleName(dto.getRoleName());
        role.setRoleType(dto.getRoleType());
        role.setDescription(dto.getDescription());
        role.setDataScope(dto.getDataScope());
        role.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        role.setSortOrder(dto.getSortOrder() == null ? 0 : dto.getSortOrder());
        roleMapper.insert(role);
        log.info("创建角色成功: roleCode={}, organCode={}, roleName={}", roleCode, dto.getOrganCode(), dto.getRoleName());

        // 4. 一并授权（可选）
        if (dto.getPermissionCodes() != null && !dto.getPermissionCodes().isEmpty()) {
            savePermissions(roleCode, dto.getPermissionCodes());
        }
        return roleCode;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(String roleCode, OrganRoleUpdateDTO dto) {
        OrganRole role = requireRole(roleCode);

        boolean changed = false;
        if (dto.getRoleName() != null) { role.setRoleName(dto.getRoleName()); changed = true; }
        if (dto.getRoleType() != null) { role.setRoleType(dto.getRoleType()); changed = true; }
        if (dto.getDescription() != null) { role.setDescription(dto.getDescription()); changed = true; }
        if (dto.getDataScope() != null) { role.setDataScope(dto.getDataScope()); changed = true; }
        if (dto.getStatus() != null) { role.setStatus(dto.getStatus()); changed = true; }
        if (dto.getSortOrder() != null) { role.setSortOrder(dto.getSortOrder()); changed = true; }

        if (changed) {
            roleMapper.updateById(role);
            log.info("更新角色成功: roleCode={}", roleCode);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(String roleCode) {
        requireRole(roleCode);

        // 校验是否有账号关联，有则拒绝
        Long relCount = accountRoleRelMapper.selectCount(new LambdaQueryWrapper<OrganAccountRoleRel>()
                .eq(OrganAccountRoleRel::getRoleCode, roleCode));
        if (relCount != null && relCount > 0) {
            throw new BusinessException(ErrorCode.BUSINESS, "角色已被账号关联，无法删除（roleCode=" + roleCode + "）");
        }

        // 删除角色-权限关联（物理删，关联表无 deleted 审计需求）
        rolePermissionShipMapper.delete(new LambdaQueryWrapper<OrganRolePermissionShip>()
                .eq(OrganRolePermissionShip::getRoleCode, roleCode));
        // 逻辑删除角色
        roleMapper.delete(new LambdaQueryWrapper<OrganRole>()
                .eq(OrganRole::getRoleCode, roleCode));
        log.info("删除角色成功: roleCode={}", roleCode);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignPermissions(String roleCode, List<String> permissionCodes) {
        requireRole(roleCode);
        // 先删后增（全量覆盖）
        rolePermissionShipMapper.delete(new LambdaQueryWrapper<OrganRolePermissionShip>()
                .eq(OrganRolePermissionShip::getRoleCode, roleCode));
        if (permissionCodes == null || permissionCodes.isEmpty()) {
            log.info("角色授权已清空: roleCode={}", roleCode);
            return;
        }
        savePermissions(roleCode, permissionCodes);
        log.info("角色授权完成: roleCode={}, 权限数={}", roleCode, permissionCodes.size());
    }

    @Override
    public List<String> listPermissions(String roleCode) {
        List<OrganRolePermissionShip> ships = rolePermissionShipMapper.selectList(
                new LambdaQueryWrapper<OrganRolePermissionShip>()
                        .eq(OrganRolePermissionShip::getRoleCode, roleCode));
        if (ships.isEmpty()) {
            return Collections.emptyList();
        }
        return ships.stream()
                .map(OrganRolePermissionShip::getPermissionCode)
                .distinct()
                .collect(Collectors.toList());
    }

    // ====== 内部方法 ======

    private OrganRole requireRole(String roleCode) {
        OrganRole role = roleMapper.selectOne(new LambdaQueryWrapper<OrganRole>()
                .eq(OrganRole::getRoleCode, roleCode)
                .last("LIMIT 1"));
        if (role == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "角色不存在：" + roleCode);
        }
        return role;
    }

    private void savePermissions(String roleCode, List<String> permissionCodes) {
        List<String> distinctCodes = permissionCodes.stream().distinct().collect(Collectors.toList());
        for (String permissionCode : distinctCodes) {
            OrganRolePermissionShip ship = new OrganRolePermissionShip();
            ship.setRoleCode(roleCode);
            ship.setPermissionCode(permissionCode);
            rolePermissionShipMapper.insert(ship);
        }
    }

    private OrganRoleSimpleVO toSimpleVO(OrganRole role) {
        OrganRoleSimpleVO vo = new OrganRoleSimpleVO();
        vo.setRoleCode(role.getRoleCode());
        vo.setRoleName(role.getRoleName());
        vo.setOrganCode(role.getOrganCode());
        vo.setRoleType(role.getRoleType());
        vo.setStatus(role.getStatus());
        vo.setSortOrder(role.getSortOrder());
        return vo;
    }

    private OrganRoleVO toVO(OrganRole role) {
        OrganRoleVO vo = new OrganRoleVO();
        vo.setId(role.getId());
        vo.setOrganCode(role.getOrganCode());
        vo.setRoleCode(role.getRoleCode());
        vo.setRoleName(role.getRoleName());
        vo.setRoleType(role.getRoleType());
        vo.setDescription(role.getDescription());
        vo.setDataScope(role.getDataScope());
        vo.setStatus(role.getStatus());
        vo.setSortOrder(role.getSortOrder());
        vo.setCreatedAt(role.getCreatedAt());
        vo.setUpdatedAt(role.getUpdatedAt());
        vo.setPermissionCodes(new ArrayList<>());
        return vo;
    }
}
