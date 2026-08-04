package com.dayan.supplier.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dayan.common.core.code.CodeGenerator;
import com.dayan.common.core.exception.BusinessException;
import com.dayan.common.core.exception.ErrorCode;
import com.dayan.common.core.resp.PageResult;
import com.dayan.supplier.dto.SupplierRoleCreateDTO;
import com.dayan.supplier.dto.SupplierRoleQueryDTO;
import com.dayan.supplier.dto.SupplierRoleUpdateDTO;
import com.dayan.supplier.entity.SupplierAccountRoleRel;
import com.dayan.supplier.entity.SupplierRole;
import com.dayan.supplier.entity.SupplierRolePermissionShip;
import com.dayan.supplier.mapper.SupplierAccountRoleRelMapper;
import com.dayan.supplier.mapper.SupplierRoleMapper;
import com.dayan.supplier.mapper.SupplierRolePermissionShipMapper;
import com.dayan.supplier.service.SupplierRoleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 供应商角色服务实现（P3 简化：基础 CRUD + 授权）。
 *
 * <p>角色编码 {@code SR + 5位序列}（如 SR00001），全局唯一；删除前校验账号关联，
 * 授权采用"先删后增"全量覆盖语义。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SupplierRoleServiceImpl implements SupplierRoleService {

    /** 角色编码前缀（Supplier Role） */
    private static final String ROLE_CODE_PREFIX = "SR";

    private final SupplierRoleMapper roleMapper;
    private final SupplierRolePermissionShipMapper rolePermissionShipMapper;
    private final SupplierAccountRoleRelMapper accountRoleRelMapper;
    private final CodeGenerator codeGenerator;

    @Override
    public PageResult<SupplierRole> page(SupplierRoleQueryDTO query) {
        LambdaQueryWrapper<SupplierRole> wrapper = new LambdaQueryWrapper<SupplierRole>()
                .eq(query.getSupplierCode() != null && !query.getSupplierCode().isEmpty(),
                        SupplierRole::getSupplierCode, query.getSupplierCode())
                .like(query.getRoleName() != null && !query.getRoleName().isEmpty(),
                        SupplierRole::getRoleName, query.getRoleName())
                .eq(query.getRoleType() != null, SupplierRole::getRoleType, query.getRoleType())
                .eq(query.getStatus() != null, SupplierRole::getStatus, query.getStatus())
                .orderByAsc(SupplierRole::getSortOrder)
                .orderByAsc(SupplierRole::getId);
        Page<SupplierRole> page = roleMapper.selectPage(
                new Page<>(query.getCurrent(), query.getSize()), wrapper);
        return new PageResult<>(query.getCurrent(), query.getSize(), page.getTotal(), page.getRecords());
    }

    @Override
    public SupplierRole getDetail(String roleCode) {
        return requireRole(roleCode);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String create(SupplierRoleCreateDTO dto) {
        String roleCode = codeGenerator.generate(ROLE_CODE_PREFIX);

        SupplierRole role = new SupplierRole();
        role.setSupplierCode(dto.getSupplierCode());
        role.setRoleCode(roleCode);
        role.setRoleName(dto.getRoleName());
        role.setRoleType(dto.getRoleType());
        role.setDescription(dto.getDescription());
        role.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        role.setSortOrder(dto.getSortOrder() == null ? 0 : dto.getSortOrder());
        roleMapper.insert(role);
        log.info("创建供应商角色成功: roleCode={}, supplierCode={}, roleName={}",
                roleCode, dto.getSupplierCode(), dto.getRoleName());
        return roleCode;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(String roleCode, SupplierRoleUpdateDTO dto) {
        SupplierRole existing = requireRole(roleCode);
        SupplierRole update = new SupplierRole();
        update.setId(existing.getId());
        if (dto.getRoleName() != null) update.setRoleName(dto.getRoleName());
        if (dto.getRoleType() != null) update.setRoleType(dto.getRoleType());
        if (dto.getDescription() != null) update.setDescription(dto.getDescription());
        if (dto.getStatus() != null) update.setStatus(dto.getStatus());
        if (dto.getSortOrder() != null) update.setSortOrder(dto.getSortOrder());
        roleMapper.updateById(update);
        log.info("更新供应商角色成功: roleCode={}", roleCode);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(String roleCode) {
        requireRole(roleCode);
        // 校验是否有账号关联
        Long relCount = accountRoleRelMapper.selectCount(new LambdaQueryWrapper<SupplierAccountRoleRel>()
                .eq(SupplierAccountRoleRel::getRoleCode, roleCode));
        if (relCount != null && relCount > 0) {
            throw new BusinessException(ErrorCode.BUSINESS,
                    "角色已被账号关联，无法删除（roleCode=" + roleCode + "）");
        }
        // 删除角色-权限关联
        rolePermissionShipMapper.delete(new LambdaQueryWrapper<SupplierRolePermissionShip>()
                .eq(SupplierRolePermissionShip::getRoleCode, roleCode));
        // 逻辑删除角色
        roleMapper.delete(new LambdaQueryWrapper<SupplierRole>()
                .eq(SupplierRole::getRoleCode, roleCode));
        log.info("删除供应商角色成功: roleCode={}", roleCode);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignPermissions(String roleCode, List<String> permissionCodes) {
        requireRole(roleCode);
        // 先删后增（全量覆盖）
        rolePermissionShipMapper.delete(new LambdaQueryWrapper<SupplierRolePermissionShip>()
                .eq(SupplierRolePermissionShip::getRoleCode, roleCode));
        if (permissionCodes == null || permissionCodes.isEmpty()) {
            log.info("供应商角色授权已清空: roleCode={}", roleCode);
            return;
        }
        List<String> distinctCodes = permissionCodes.stream().distinct().collect(Collectors.toList());
        for (String permissionCode : distinctCodes) {
            SupplierRolePermissionShip ship = new SupplierRolePermissionShip();
            ship.setRoleCode(roleCode);
            ship.setPermissionCode(permissionCode);
            rolePermissionShipMapper.insert(ship);
        }
        log.info("供应商角色授权完成: roleCode={}, 权限数={}", roleCode, distinctCodes.size());
    }

    @Override
    public List<String> listPermissions(String roleCode) {
        List<SupplierRolePermissionShip> ships = rolePermissionShipMapper.selectList(
                new LambdaQueryWrapper<SupplierRolePermissionShip>()
                        .eq(SupplierRolePermissionShip::getRoleCode, roleCode));
        if (ships.isEmpty()) {
            return Collections.emptyList();
        }
        return ships.stream()
                .map(SupplierRolePermissionShip::getPermissionCode)
                .distinct()
                .collect(Collectors.toList());
    }

    private SupplierRole requireRole(String roleCode) {
        SupplierRole role = roleMapper.selectOne(new LambdaQueryWrapper<SupplierRole>()
                .eq(SupplierRole::getRoleCode, roleCode)
                .last("LIMIT 1"));
        if (role == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "供应商角色不存在: " + roleCode);
        }
        return role;
    }
}
