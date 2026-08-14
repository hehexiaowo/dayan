package com.dayan.organ.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dayan.common.core.exception.BusinessException;
import com.dayan.common.core.exception.ErrorCode;
import com.dayan.common.core.resp.PageResult;
import com.dayan.common.security.password.PasswordService;
import com.dayan.common.security.secret.DayanSecrets;
import com.dayan.organ.entity.OrganAccount;
import com.dayan.organ.entity.OrganAccountRoleRel;
import com.dayan.organ.entity.OrganInfo;
import com.dayan.organ.entity.OrganRole;
import com.dayan.organ.mapper.OrganAccountMapper;
import com.dayan.organ.mapper.OrganAccountRoleRelMapper;
import com.dayan.organ.mapper.OrganInfoMapper;
import com.dayan.organ.mapper.OrganRoleMapper;
import com.dayan.organ.vo.OrganAccountVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 核心账号管理服务（Admin 端 organ_account CRUD + 重置密码 + 状态切换）。
 */
@Service
@RequiredArgsConstructor
public class OrganAccountService {

    /** 重置密码默认值：由 DayanSecrets 单点配置（生产必须显式配置） */
    private final DayanSecrets dayanSecrets;

    private final OrganAccountMapper accountMapper;
    private final PasswordService passwordService;
    private final OrganInfoMapper organInfoMapper;
    private final OrganRoleMapper roleMapper;
    private final OrganAccountRoleRelMapper accountRoleRelMapper;

    public PageResult<OrganAccountVO> page(String organCode, String username, String realName,
                                           Integer accountStatus, long current, long size) {
        LambdaQueryWrapper<OrganAccount> wrapper = new LambdaQueryWrapper<OrganAccount>()
                .orderByDesc(OrganAccount::getCreatedAt);
        // organCode 为空时不加过滤（超管可查看全部机构账号）
        if (organCode != null && !organCode.isEmpty()) {
            wrapper.eq(OrganAccount::getOrganCode, organCode);
        }
        if (username != null && !username.isEmpty()) {
            wrapper.and(w -> w.eq(OrganAccount::getUsername, username)
                    .or().eq(OrganAccount::getPhone, username)
                    .or().eq(OrganAccount::getEmail, username));
        }
        if (realName != null && !realName.isEmpty()) {
            wrapper.like(OrganAccount::getRealName, realName);
        }
        if (accountStatus != null) {
            wrapper.eq(OrganAccount::getAccountStatus, accountStatus);
        }
        Page<OrganAccount> page = accountMapper.selectPage(new Page<>(current, size), wrapper);
        List<OrganAccount> accounts = page.getRecords();

        // 批量解析机构名 + 角色，避免 N+1（VO 不含 password/salt，天然脱敏）
        Map<String, String> organNameMap = resolveOrganNames(accounts);
        Map<String, List<String>> accountRoles = resolveAccountRoleCodes(accounts);
        Set<String> allRoleCodes = accountRoles.values().stream()
                .flatMap(List::stream)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<String, String> roleNameMap = resolveRoleNames(allRoleCodes);

        List<OrganAccountVO> records = accounts.stream()
                .map(a -> toVO(a, organNameMap, accountRoles, roleNameMap))
                .toList();
        return new PageResult<>(current, size, page.getTotal(), records);
    }

    public OrganAccountVO getDetail(String accountCode) {
        OrganAccount account = selectByCode(accountCode);
        Map<String, String> organNameMap = resolveOrganNames(List.of(account));
        Map<String, List<String>> accountRoles = resolveAccountRoleCodes(List.of(account));
        Map<String, String> roleNameMap = resolveRoleNames(
                accountRoles.getOrDefault(accountCode, Collections.emptyList()));
        return toVO(account, organNameMap, accountRoles, roleNameMap);
    }

    @Transactional(rollbackFor = Exception.class)
    public String create(OrganAccount account) {
        // username 唯一校验
        Long count = accountMapper.selectCount(new LambdaQueryWrapper<OrganAccount>()
                .eq(OrganAccount::getUsername, account.getUsername()));
        if (count > 0) {
            throw new BusinessException(ErrorCode.BUSINESS, "用户名已存在: " + account.getUsername());
        }
        // 生成 accountCode
        account.setAccountCode(generateAccountCode());
        // BCrypt 哈希密码
        String rawPwd = account.getPassword() == null ? dayanSecrets.getDefaultResetPassword() : account.getPassword();
        account.setPassword(passwordService.encode(rawPwd));
        account.setSalt("bcrypt");
        if (account.getAccountStatus() == null) account.setAccountStatus(1);
        if (account.getIsAdmin() == null) account.setIsAdmin(0);
        if (account.getLoginCount() == null) account.setLoginCount(0);
        accountMapper.insert(account);
        return account.getAccountCode();
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(String accountCode, OrganAccount account) {
        OrganAccount existing = selectByCode(accountCode);
        account.setId(existing.getId());
        // 不允许通过 update 改密码
        account.setPassword(null);
        accountMapper.updateById(account);
    }

    /**
     * 重置密码为默认值（管理员操作）。
     */
    @Transactional(rollbackFor = Exception.class)
    public void resetPassword(String accountCode) {
        OrganAccount existing = selectByCode(accountCode);
        OrganAccount update = new OrganAccount();
        update.setId(existing.getId());
        update.setPassword(passwordService.encode(dayanSecrets.getDefaultResetPassword()));
        accountMapper.updateById(update);
    }

    /**
     * 切换账号状态（0=锁定, 1=正常, 2=禁用）。
     */
    @Transactional(rollbackFor = Exception.class)
    public void switchStatus(String accountCode, Integer status) {
        if (status == null || status < 0 || status > 2) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "状态值非法: " + status);
        }
        OrganAccount existing = selectByCode(accountCode);
        if (existing.getIsAdmin() != null && existing.getIsAdmin() == 1) {
            throw new BusinessException(ErrorCode.BUSINESS, "超级管理员不可切换状态");
        }
        OrganAccount update = new OrganAccount();
        update.setId(existing.getId());
        update.setAccountStatus(status);
        accountMapper.updateById(update);
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(String accountCode) {
        OrganAccount existing = selectByCode(accountCode);
        if (existing.getIsAdmin() != null && existing.getIsAdmin() == 1) {
            throw new BusinessException(ErrorCode.BUSINESS, "超级管理员不可删除");
        }
        accountMapper.deleteById(existing.getId());
    }

    private OrganAccount selectByCode(String accountCode) {
        OrganAccount account = accountMapper.selectOne(new LambdaQueryWrapper<OrganAccount>()
                .eq(OrganAccount::getAccountCode, accountCode).last("LIMIT 1"));
        if (account == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "账号不存在: " + accountCode);
        }
        return account;
    }

    /** 批量解析 organCode → 机构名（优先全称，回退简称）。 */
    private Map<String, String> resolveOrganNames(List<OrganAccount> accounts) {
        Set<String> organCodes = accounts.stream()
                .map(OrganAccount::getOrganCode)
                .filter(c -> c != null && !c.isEmpty())
                .collect(Collectors.toSet());
        if (organCodes.isEmpty()) {
            return Collections.emptyMap();
        }
        List<OrganInfo> organs = organInfoMapper.selectList(new LambdaQueryWrapper<OrganInfo>()
                .in(OrganInfo::getOrganCode, organCodes));
        return organs.stream()
                .collect(Collectors.toMap(OrganInfo::getOrganCode,
                        o -> o.getFullName() != null && !o.getFullName().isEmpty()
                                ? o.getFullName() : o.getShortName(),
                        (x, y) -> x));
    }

    /** 批量解析 accountCode → roleCode 列表。 */
    private Map<String, List<String>> resolveAccountRoleCodes(List<OrganAccount> accounts) {
        Set<String> accountCodes = accounts.stream()
                .map(OrganAccount::getAccountCode)
                .filter(c -> c != null && !c.isEmpty())
                .collect(Collectors.toSet());
        if (accountCodes.isEmpty()) {
            return Collections.emptyMap();
        }
        List<OrganAccountRoleRel> rels = accountRoleRelMapper.selectList(
                new LambdaQueryWrapper<OrganAccountRoleRel>()
                        .in(OrganAccountRoleRel::getAccountCode, accountCodes));
        return rels.stream()
                .collect(Collectors.groupingBy(OrganAccountRoleRel::getAccountCode,
                        Collectors.mapping(OrganAccountRoleRel::getRoleCode, Collectors.toList())));
    }

    /** 批量解析 roleCode → roleName。 */
    private Map<String, String> resolveRoleNames(Collection<String> roleCodes) {
        if (roleCodes == null || roleCodes.isEmpty()) {
            return Collections.emptyMap();
        }
        List<OrganRole> roles = roleMapper.selectList(new LambdaQueryWrapper<OrganRole>()
                .in(OrganRole::getRoleCode, roleCodes));
        return roles.stream()
                .collect(Collectors.toMap(OrganRole::getRoleCode, OrganRole::getRoleName, (x, y) -> x));
    }

    private OrganAccountVO toVO(OrganAccount a, Map<String, String> organNameMap,
                                Map<String, List<String>> accountRoles, Map<String, String> roleNameMap) {
        OrganAccountVO vo = new OrganAccountVO();
        vo.setId(a.getId());
        vo.setOrganCode(a.getOrganCode());
        vo.setOrganName(organNameMap.get(a.getOrganCode()));
        vo.setAccountCode(a.getAccountCode());
        vo.setUsername(a.getUsername());
        vo.setRealName(a.getRealName());
        vo.setAvatar(a.getAvatar());
        vo.setGender(a.getGender());
        vo.setPhone(a.getPhone());
        vo.setEmail(a.getEmail());
        vo.setLastLoginTime(a.getLastLoginTime());
        vo.setLastLoginIp(a.getLastLoginIp());
        vo.setLoginCount(a.getLoginCount());
        vo.setPwdUpdateTime(a.getPwdUpdateTime());
        vo.setAccountStatus(a.getAccountStatus());
        vo.setIsAdmin(a.getIsAdmin());
        vo.setRemark(a.getRemark());
        List<String> roleCodes = accountRoles.getOrDefault(a.getAccountCode(), Collections.emptyList());
        vo.setRoleCodes(roleCodes);
        vo.setRoleNames(roleCodes.stream()
                .map(roleNameMap::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList()));
        return vo;
    }

    private String generateAccountCode() {
        long ts = System.currentTimeMillis() % 100000L;
        int rand = (int) (Math.random() * 1000);
        return String.format("AC%05d%03d", ts, rand);
    }
}
