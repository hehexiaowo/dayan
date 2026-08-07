package com.dayan.channel.security;

import cn.dev33.satoken.stp.StpInterface;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dayan.channel.entity.ChannelAccount;
import com.dayan.channel.entity.ChannelAccountRoleRel;
import com.dayan.channel.entity.ChannelPermission;
import com.dayan.channel.entity.ChannelRolePermissionShip;
import com.dayan.channel.mapper.ChannelAccountMapper;
import com.dayan.channel.mapper.ChannelAccountRoleRelMapper;
import com.dayan.channel.mapper.ChannelPermissionMapper;
import com.dayan.channel.mapper.ChannelRolePermissionShipMapper;
import com.dayan.common.security.AccountType;
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
 * Sa-Token 权限/角色查询实现（channel 域，渠道端）。
 *
 * <p>Sa-Token 在 @SaCheckPermission / @SaCheckRole 注解校验时自动回调本实现，
 * 由 loginType 区分端：仅处理 Channel 端（loginType=channel），其余端返回空。
 *
 * <p>查询链路：
 * <ol>
 *   <li>超管（channel_account.is_admin=1）→ 权限返回 ["*"]（通配）、角色额外返回 ROLE_CHANNEL_ADMIN</li>
 *   <li>普通账号：account_code → channel_account_role_rel → role_code 列表</li>
 *   <li>role_code → channel_role_permission_ship → permission_code 列表（去重）</li>
 *   <li>permission_code → channel_permission(status!=0) 过滤停用权限</li>
 * </ol>
 *
 * <p>与 organ 域的 {@link com.dayan.organ.security.DayanStpInterface} 共存：
 * 两者各自按 loginType 自过滤，Sa-Token 按 loginType 分发，互不干扰。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChannelStpInterface implements StpInterface {

    /** 渠道超管角色标识（在角色集合中额外返回，便于 @SaCheckRole 兜底） */
    public static final String ROLE_CHANNEL_ADMIN = "ROLE_CHANNEL_ADMIN";
    /** 通配权限码，超管返回此值后所有 @SaCheckPermission 自动通过 */
    public static final String WILDCARD_PERMISSION = "*";

    private final ChannelAccountMapper accountMapper;
    private final ChannelAccountRoleRelMapper accountRoleRelMapper;
    private final ChannelRolePermissionShipMapper rolePermissionShipMapper;
    private final ChannelPermissionMapper permissionMapper;

    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        // 仅 Channel 端走 channel 权限体系；其余端不处理
        if (!isChannelLoginType(loginType)) {
            return Collections.emptyList();
        }
        if (loginId == null) {
            return Collections.emptyList();
        }
        String accountCode = loginId.toString();

        // 1. 查账号，超管返回通配权限
        ChannelAccount account = accountMapper.selectOne(new LambdaQueryWrapper<ChannelAccount>()
                .eq(ChannelAccount::getAccountCode, accountCode)
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
        List<ChannelRolePermissionShip> ships = rolePermissionShipMapper.selectList(
                new LambdaQueryWrapper<ChannelRolePermissionShip>()
                        .in(ChannelRolePermissionShip::getRoleCode, roleCodes));
        if (ships.isEmpty()) {
            return Collections.emptyList();
        }
        Set<String> permissionCodes = ships.stream()
                .map(ChannelRolePermissionShip::getPermissionCode)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        // 4. 仅返回启用状态的权限码（status != 0 视为启用，过滤已停用权限）
        List<ChannelPermission> permissions = permissionMapper.selectList(
                new LambdaQueryWrapper<ChannelPermission>()
                        .in(ChannelPermission::getPermissionCode, permissionCodes)
                        .ne(ChannelPermission::getStatus, 0));
        if (permissions.isEmpty()) {
            return Collections.emptyList();
        }
        return permissions.stream()
                .map(ChannelPermission::getPermissionCode)
                .distinct()
                .collect(Collectors.toList());
    }

    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        // 仅 Channel 端走 channel 权限体系；其余端不处理
        if (!isChannelLoginType(loginType)) {
            return Collections.emptyList();
        }
        if (loginId == null) {
            return Collections.emptyList();
        }
        String accountCode = loginId.toString();

        List<String> roleCodes = new ArrayList<>(listRoleCodes(accountCode));

        // 超管额外返回 ROLE_CHANNEL_ADMIN，便于 @SaCheckRole 兜底
        ChannelAccount account = accountMapper.selectOne(new LambdaQueryWrapper<ChannelAccount>()
                .eq(ChannelAccount::getAccountCode, accountCode)
                .last("LIMIT 1"));
        if (account != null && account.getIsAdmin() != null && account.getIsAdmin() == 1
                && !roleCodes.contains(ROLE_CHANNEL_ADMIN)) {
            roleCodes.add(ROLE_CHANNEL_ADMIN);
        }
        return roleCodes;
    }

    /**
     * 查询账号关联的角色编码列表（去重）。
     */
    private List<String> listRoleCodes(String accountCode) {
        List<ChannelAccountRoleRel> rels = accountRoleRelMapper.selectList(
                new LambdaQueryWrapper<ChannelAccountRoleRel>()
                        .eq(ChannelAccountRoleRel::getAccountCode, accountCode));
        if (rels.isEmpty()) {
            return Collections.emptyList();
        }
        return rels.stream()
                .map(ChannelAccountRoleRel::getRoleCode)
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * 判断是否为 Channel 端登录类型。
     *
     * <p>仅接受 "channel"（{@link AccountType#CHANNEL} 的 loginType）。
     * channel-api 的 @SaCheckPermission 由 StpKit.CHANNEL 解析，loginType 固定为 "channel"。
     */
    private boolean isChannelLoginType(String loginType) {
        return AccountType.CHANNEL.getLoginType().equals(loginType);
    }
}
