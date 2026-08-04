package com.dayan.organ.security;

import cn.dev33.satoken.stp.StpInterface;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dayan.common.security.AccountType;
import com.dayan.organ.entity.OrganAccount;
import com.dayan.organ.entity.OrganAccountRoleRel;
import com.dayan.organ.entity.OrganPermission;
import com.dayan.organ.entity.OrganRolePermissionShip;
import com.dayan.organ.mapper.OrganAccountMapper;
import com.dayan.organ.mapper.OrganAccountRoleRelMapper;
import com.dayan.organ.mapper.OrganPermissionMapper;
import com.dayan.organ.mapper.OrganRolePermissionShipMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Sa-Token 权限/角色查询实现（organ 域，Admin 端）。
 *
 * <p>Sa-Token 在 {@code @SaCheckPermission} / {@code @SaCheckRole} 注解校验时自动回调本实现，
 * 由 {@code loginType} 区分端：仅处理 Admin 端（{@code loginType=admin}），其余端 P1 暂不处理返回空。
 *
 * <p>查询链路：
 * <ol>
 *   <li>超管（organ_account.is_admin=1）→ 权限返回 {@code ["*"]}（通配）、角色额外返回 {@code ROLE_SUPER_ADMIN}</li>
 *   <li>普通账号：account_code → organ_account_role_rel → role_code 列表</li>
 *   <li>role_code → organ_role_permission_ship → permission_code 列表（去重）</li>
 * </ol>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DayanStpInterface implements StpInterface {

    /** 超级管理员角色标识（在角色集合中额外返回，便于 @SaCheckRole("ROLE_SUPER_ADMIN") 兜底） */
    public static final String ROLE_SUPER_ADMIN = "ROLE_SUPER_ADMIN";
    /** 通配权限码，超管返回此值后所有 @SaCheckPermission 自动通过 */
    public static final String WILDCARD_PERMISSION = "*";

    private final OrganAccountMapper accountMapper;
    private final OrganAccountRoleRelMapper accountRoleRelMapper;
    private final OrganRolePermissionShipMapper rolePermissionShipMapper;
    private final OrganPermissionMapper permissionMapper;

    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        // 仅 Admin 端走 organ 权限体系；其余端 P1 暂不处理
        if (!AccountType.ADMIN.getLoginType().equals(loginType)) {
            return Collections.emptyList();
        }
        if (loginId == null) {
            return Collections.emptyList();
        }
        String accountCode = loginId.toString();

        // 1. 查账号，超管返回通配权限
        OrganAccount account = accountMapper.selectOne(new LambdaQueryWrapper<OrganAccount>()
                .eq(OrganAccount::getAccountCode, accountCode)
                .last("LIMIT 1"));
        if (account == null) {
            return Collections.emptyList();
        }
        if (account.getIsAdmin() != null && account.getIsAdmin() == 1) {
            return Collections.singletonList(WILDCARD_PERMISSION);
        }

        // 2. account_code → role_code 列表
        List<String> roleCodes = listRoleCodes(accountCode);
        if (roleCodes.isEmpty()) {
            return Collections.emptyList();
        }

        // 3. role_code → permission_code 列表（去重，保持顺序）
        List<OrganRolePermissionShip> ships = rolePermissionShipMapper.selectList(
                new LambdaQueryWrapper<OrganRolePermissionShip>()
                        .in(OrganRolePermissionShip::getRoleCode, roleCodes));
        if (ships.isEmpty()) {
            return Collections.emptyList();
        }
        Set<String> permissionCodes = ships.stream()
                .map(OrganRolePermissionShip::getPermissionCode)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        // 4. 仅返回启用状态的权限码（status != 0 视为启用，过滤已停用权限）
        List<OrganPermission> permissions = permissionMapper.selectList(
                new LambdaQueryWrapper<OrganPermission>()
                        .in(OrganPermission::getPermissionCode, permissionCodes)
                        .ne(OrganPermission::getStatus, 0));
        if (permissions.isEmpty()) {
            return Collections.emptyList();
        }
        return permissions.stream()
                .map(OrganPermission::getPermissionCode)
                .distinct()
                .collect(Collectors.toList());
    }

    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        // 仅 Admin 端走 organ 权限体系；其余端 P1 暂不处理
        if (!AccountType.ADMIN.getLoginType().equals(loginType)) {
            return Collections.emptyList();
        }
        if (loginId == null) {
            return Collections.emptyList();
        }
        String accountCode = loginId.toString();

        List<String> roleCodes = new ArrayList<>(listRoleCodes(accountCode));

        // 超管额外返回 ROLE_SUPER_ADMIN，便于 @SaCheckRole 兜底
        OrganAccount account = accountMapper.selectOne(new LambdaQueryWrapper<OrganAccount>()
                .eq(OrganAccount::getAccountCode, accountCode)
                .last("LIMIT 1"));
        if (account != null && account.getIsAdmin() != null && account.getIsAdmin() == 1
                && !roleCodes.contains(ROLE_SUPER_ADMIN)) {
            roleCodes.add(ROLE_SUPER_ADMIN);
        }
        return roleCodes;
    }

    /**
     * 查询账号关联的角色编码列表（去重）。
     */
    private List<String> listRoleCodes(String accountCode) {
        List<OrganAccountRoleRel> rels = accountRoleRelMapper.selectList(
                new LambdaQueryWrapper<OrganAccountRoleRel>()
                        .eq(OrganAccountRoleRel::getAccountCode, accountCode));
        if (rels.isEmpty()) {
            return Collections.emptyList();
        }
        return rels.stream()
                .map(OrganAccountRoleRel::getRoleCode)
                .distinct()
                .collect(Collectors.toList());
    }
}
