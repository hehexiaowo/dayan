package com.dayan.organ.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dayan.common.core.exception.BusinessException;
import com.dayan.common.core.exception.ErrorCode;
import com.dayan.common.core.resp.PageResult;
import com.dayan.common.security.password.PasswordService;
import com.dayan.organ.entity.OrganAccount;
import com.dayan.organ.mapper.OrganAccountMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 核心账号管理服务（Admin 端 organ_account CRUD + 重置密码 + 状态切换）。
 */
@Service
@RequiredArgsConstructor
public class OrganAccountService {

    /** 重置密码默认值 */
    private static final String DEFAULT_RESET_PASSWORD = "dayan@123";

    private final OrganAccountMapper accountMapper;
    private final PasswordService passwordService;

    public PageResult<OrganAccount> page(String organCode, String username, String realName,
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
        // 脱敏密码（不返回）
        page.getRecords().forEach(a -> a.setPassword(null));
        return new PageResult<>(current, size, page.getTotal(), page.getRecords());
    }

    public OrganAccount getDetail(String accountCode) {
        OrganAccount account = selectByCode(accountCode);
        account.setPassword(null);
        return account;
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
        String rawPwd = account.getPassword() == null ? DEFAULT_RESET_PASSWORD : account.getPassword();
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
        update.setPassword(passwordService.encode(DEFAULT_RESET_PASSWORD));
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

    private String generateAccountCode() {
        long ts = System.currentTimeMillis() % 100000L;
        int rand = (int) (Math.random() * 1000);
        return String.format("AC%05d%03d", ts, rand);
    }
}
