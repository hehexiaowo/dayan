package com.dayan.organ.service.impl;

import cn.dev33.satoken.stp.StpLogic;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dayan.common.core.exception.AccountLockedException;
import com.dayan.common.core.exception.BusinessException;
import com.dayan.common.core.exception.ErrorCode;
import com.dayan.common.redis.RedisKey;
import com.dayan.common.security.AccountType;
import com.dayan.common.security.StpKit;
import com.dayan.common.security.password.PasswordService;
import com.dayan.organ.dto.AuthLoginDTO;
import com.dayan.organ.entity.OrganAccount;
import com.dayan.organ.mapper.OrganAccountMapper;
import com.dayan.organ.service.AdminAuthService;
import com.dayan.organ.vo.AuthLoginVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

/**
 * Admin 运营端认证服务实现。
 *
 * <p>登录流程：
 * <ol>
 *   <li>按 username/phone/email 任一查询 organ_account</li>
 *   <li>校验账号状态（锁定/禁用）</li>
 *   <li>BCrypt 校验密码</li>
 *   <li>Sa-Token login（loginType=admin，独立命名空间）</li>
 *   <li>更新登录时间/次数</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminAuthServiceImpl implements AdminAuthService {

    private final OrganAccountMapper accountMapper;
    private final PasswordService passwordService;
    private final StringRedisTemplate redisTemplate;

    /** 登录失败锁定阈值 */
    private static final int FAIL_LIMIT = 5;
    /** 锁定时长（分钟） */
    private static final int LOCK_MINUTES = 30;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AuthLoginVO login(AuthLoginDTO dto) {
        // 1. 按 username/phone/email 查询账号
        OrganAccount account = accountMapper.selectOne(new LambdaQueryWrapper<OrganAccount>()
                .eq(OrganAccount::getUsername, dto.getUsername())
                .or()
                .eq(OrganAccount::getPhone, dto.getUsername())
                .or()
                .eq(OrganAccount::getEmail, dto.getUsername())
                .last("LIMIT 1"));
        if (account == null) {
            throw new BusinessException(ErrorCode.BUSINESS, "账号或密码错误");
        }

        // 2. 校验登录失败锁定（Redis 计数）
        String failKey = RedisKey.authFail("admin", dto.getUsername());
        String failCountStr = redisTemplate.opsForValue().get(failKey);
        if (failCountStr != null && Integer.parseInt(failCountStr) >= FAIL_LIMIT) {
            throw new AccountLockedException("登录失败次数过多，账号已锁定 " + LOCK_MINUTES + " 分钟");
        }

        // 3. 校验账号状态
        if (account.getAccountStatus() != null && account.getAccountStatus() == 0) {
            throw new AccountLockedException("账号已被锁定，请联系管理员");
        }
        if (account.getAccountStatus() != null && account.getAccountStatus() == 2) {
            throw new BusinessException(ErrorCode.BUSINESS, "账号已被禁用");
        }

        // 4. BCrypt 校验密码
        if (!passwordService.matches(dto.getPassword(), account.getPassword())) {
            // 失败计数 +1
            Long count = redisTemplate.opsForValue().increment(failKey);
            if (count != null && count == 1L) {
                redisTemplate.expire(failKey, LOCK_MINUTES, TimeUnit.MINUTES);
            }
            int remaining = FAIL_LIMIT - (count == null ? 0 : count.intValue());
            String msg = remaining > 0
                    ? "账号或密码错误，剩余尝试次数 " + remaining
                    : "登录失败次数过多，账号已锁定 " + LOCK_MINUTES + " 分钟";
            throw new BusinessException(ErrorCode.BUSINESS, msg);
        }

        // 5. 登录成功，清除失败计数
        redisTemplate.delete(failKey);

        // 6. Sa-Token 登录（admin 命名空间）
        StpLogic logic = StpKit.ADMIN;
        logic.login(account.getAccountCode());
        // Session 存入渠道/组织信息（Admin 端无 channel_code，存 organ_code 便于审计）
        logic.getSession().set("organCode", account.getOrganCode());
        logic.getSession().set("accountType", AccountType.ADMIN.getLoginType());

        // 7. 更新登录信息
        OrganAccount update = new OrganAccount();
        update.setId(account.getId());
        update.setLoginCount((account.getLoginCount() == null ? 0 : account.getLoginCount()) + 1);
        update.setLastLoginTime(java.time.LocalDateTime.now());
        accountMapper.updateById(update);

        log.info("Admin 登录成功: accountCode={}, username={}", account.getAccountCode(), account.getUsername());

        return AuthLoginVO.builder()
                .token(logic.getTokenValue())
                .tokenName(AccountType.ADMIN.getTokenName())
                .accountCode(account.getAccountCode())
                .realName(account.getRealName())
                .avatar(account.getAvatar())
                .isAdmin(account.getIsAdmin() != null && account.getIsAdmin() == 1)
                .build();
    }

    @Override
    public void logout() {
        StpKit.ADMIN.logout();
    }

    @Override
    public AuthLoginVO current() {
        StpLogic logic = StpKit.ADMIN;
        Object loginId = logic.getLoginIdDefaultNull();
        if (loginId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        OrganAccount account = accountMapper.selectOne(new LambdaQueryWrapper<OrganAccount>()
                .eq(OrganAccount::getAccountCode, loginId.toString())
                .last("LIMIT 1"));
        if (account == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "账号不存在");
        }
        return AuthLoginVO.builder()
                .token(logic.getTokenValue())
                .tokenName(AccountType.ADMIN.getTokenName())
                .accountCode(account.getAccountCode())
                .realName(account.getRealName())
                .avatar(account.getAvatar())
                .isAdmin(account.getIsAdmin() != null && account.getIsAdmin() == 1)
                .build();
    }
}
