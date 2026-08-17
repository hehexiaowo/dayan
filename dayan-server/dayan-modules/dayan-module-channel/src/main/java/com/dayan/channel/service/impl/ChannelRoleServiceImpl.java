package com.dayan.channel.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dayan.channel.dto.ChannelRoleCreateDTO;
import com.dayan.channel.dto.ChannelRoleQueryDTO;
import com.dayan.channel.dto.ChannelRoleUpdateDTO;
import com.dayan.channel.entity.ChannelAccountRoleRel;
import com.dayan.channel.entity.ChannelRole;
import com.dayan.channel.entity.ChannelRolePermissionShip;
import com.dayan.channel.mapper.ChannelAccountRoleRelMapper;
import com.dayan.channel.mapper.ChannelRoleMapper;
import com.dayan.channel.mapper.ChannelRolePermissionShipMapper;
import com.dayan.channel.service.ChannelRoleService;
import com.dayan.common.core.code.CodeGenerator;
import com.dayan.common.core.exception.BusinessException;
import com.dayan.common.core.exception.ErrorCode;
import com.dayan.common.core.resp.PageResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 渠道角色服务实现（P2 简化：基础 CRUD + 授权）。
 *
 * <p>角色编码 {@code RL + 5位序列}（如 RL00001），全局唯一；删除前校验账号关联，
 * 授权采用"先删后增"全量覆盖语义。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChannelRoleServiceImpl implements ChannelRoleService {

    /** 角色编码前缀（RoLe） */
    private static final String ROLE_CODE_PREFIX = "RL";

    /** 内置角色占位渠道编码：channel_code='GLOBAL' 的行是全渠道共用的系统预置角色 */
    public static final String GLOBAL_CHANNEL_CODE = "GLOBAL";

    private final ChannelRoleMapper roleMapper;
    private final ChannelRolePermissionShipMapper rolePermissionShipMapper;
    private final ChannelAccountRoleRelMapper accountRoleRelMapper;
    private final CodeGenerator codeGenerator;

    @Override
    public PageResult<ChannelRole> page(ChannelRoleQueryDTO query) {
        LambdaQueryWrapper<ChannelRole> wrapper = new LambdaQueryWrapper<ChannelRole>()
                .like(query.getRoleName() != null && !query.getRoleName().isEmpty(),
                        ChannelRole::getRoleName, query.getRoleName())
                .eq(query.getRoleType() != null, ChannelRole::getRoleType, query.getRoleType())
                .eq(query.getStatus() != null, ChannelRole::getStatus, query.getStatus())
                .orderByAsc(ChannelRole::getSortOrder)
                .orderByAsc(ChannelRole::getId);
        // 渠道维度：内置角色（GLOBAL）全渠道共用 + 本渠道自定义角色；
        // admin 端不传 channelCode 时查全部
        if (query.getChannelCode() != null && !query.getChannelCode().isEmpty()) {
            wrapper.in(ChannelRole::getChannelCode, GLOBAL_CHANNEL_CODE, query.getChannelCode());
        }
        Page<ChannelRole> page = roleMapper.selectPage(
                new Page<>(query.getCurrent(), query.getSize()), wrapper);
        return new PageResult<>(query.getCurrent(), query.getSize(), page.getTotal(), page.getRecords());
    }

    @Override
    public ChannelRole getDetail(String roleCode) {
        return requireRole(roleCode);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String create(ChannelRoleCreateDTO dto) {
        // 内置角色由平台维护（channel_code='GLOBAL'），渠道只能创建自定义角色
        if (dto.getRoleType() != null && dto.getRoleType() == 1) {
            throw new BusinessException(ErrorCode.BUSINESS, "内置角色由平台统一维护，不能创建");
        }
        String roleCode = codeGenerator.generate(ROLE_CODE_PREFIX);

        ChannelRole role = new ChannelRole();
        role.setChannelCode(dto.getChannelCode());
        role.setRoleCode(roleCode);
        role.setRoleName(dto.getRoleName());
        role.setRoleType(dto.getRoleType() == null ? 2 : dto.getRoleType());
        role.setDescription(dto.getDescription());
        role.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        role.setSortOrder(dto.getSortOrder() == null ? 0 : dto.getSortOrder());
        roleMapper.insert(role);
        log.info("创建渠道角色成功: roleCode={}, channelCode={}, roleName={}",
                roleCode, dto.getChannelCode(), dto.getRoleName());
        return roleCode;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(String roleCode, ChannelRoleUpdateDTO dto) {
        ChannelRole existing = requireRole(roleCode);
        if (existing.getRoleType() != null && existing.getRoleType() == 1) {
            throw new BusinessException(ErrorCode.BUSINESS, "内置角色不可修改");
        }
        ChannelRole update = new ChannelRole();
        update.setId(existing.getId());
        if (dto.getRoleName() != null) update.setRoleName(dto.getRoleName());
        if (dto.getRoleType() != null) update.setRoleType(dto.getRoleType());
        if (dto.getDescription() != null) update.setDescription(dto.getDescription());
        if (dto.getStatus() != null) update.setStatus(dto.getStatus());
        if (dto.getSortOrder() != null) update.setSortOrder(dto.getSortOrder());
        roleMapper.updateById(update);
        log.info("更新渠道角色成功: roleCode={}", roleCode);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(String roleCode) {
        ChannelRole existing = requireRole(roleCode);
        if (existing.getRoleType() != null && existing.getRoleType() == 1) {
            throw new BusinessException(ErrorCode.BUSINESS, "内置角色不可删除");
        }
        // 校验是否有账号关联
        Long relCount = accountRoleRelMapper.selectCount(new LambdaQueryWrapper<ChannelAccountRoleRel>()
                .eq(ChannelAccountRoleRel::getRoleCode, roleCode));
        if (relCount != null && relCount > 0) {
            throw new BusinessException(ErrorCode.BUSINESS,
                    "角色已被账号关联，无法删除（roleCode=" + roleCode + "）");
        }
        // 删除角色-权限关联
        rolePermissionShipMapper.delete(new LambdaQueryWrapper<ChannelRolePermissionShip>()
                .eq(ChannelRolePermissionShip::getRoleCode, roleCode));
        // 逻辑删除角色
        roleMapper.delete(new LambdaQueryWrapper<ChannelRole>()
                .eq(ChannelRole::getRoleCode, roleCode));
        log.info("删除渠道角色成功: roleCode={}", roleCode);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignPermissions(String roleCode, List<String> permissionCodes) {
        requireRole(roleCode);
        // 先删后增（全量覆盖）
        rolePermissionShipMapper.delete(new LambdaQueryWrapper<ChannelRolePermissionShip>()
                .eq(ChannelRolePermissionShip::getRoleCode, roleCode));
        if (permissionCodes == null || permissionCodes.isEmpty()) {
            log.info("渠道角色授权已清空: roleCode={}", roleCode);
            return;
        }
        List<String> distinctCodes = permissionCodes.stream().distinct().collect(Collectors.toList());
        for (String permissionCode : distinctCodes) {
            ChannelRolePermissionShip ship = new ChannelRolePermissionShip();
            ship.setRoleCode(roleCode);
            ship.setPermissionCode(permissionCode);
            rolePermissionShipMapper.insert(ship);
        }
        log.info("渠道角色授权完成: roleCode={}, 权限数={}", roleCode, distinctCodes.size());
    }

    @Override
    public List<String> listPermissions(String roleCode) {
        List<ChannelRolePermissionShip> ships = rolePermissionShipMapper.selectList(
                new LambdaQueryWrapper<ChannelRolePermissionShip>()
                        .eq(ChannelRolePermissionShip::getRoleCode, roleCode));
        if (ships.isEmpty()) {
            return Collections.emptyList();
        }
        return ships.stream()
                .map(ChannelRolePermissionShip::getPermissionCode)
                .distinct()
                .collect(Collectors.toList());
    }

    private ChannelRole requireRole(String roleCode) {
        ChannelRole role = roleMapper.selectOne(new LambdaQueryWrapper<ChannelRole>()
                .eq(ChannelRole::getRoleCode, roleCode)
                .last("LIMIT 1"));
        if (role == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "渠道角色不存在: " + roleCode);
        }
        return role;
    }
}
