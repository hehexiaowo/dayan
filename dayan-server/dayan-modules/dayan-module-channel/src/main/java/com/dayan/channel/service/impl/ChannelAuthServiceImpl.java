package com.dayan.channel.service.impl;

import cn.dev33.satoken.stp.StpLogic;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dayan.channel.dto.AuthLoginDTO;
import com.dayan.channel.entity.ChannelAccount;
import com.dayan.channel.mapper.ChannelAccountMapper;
import com.dayan.channel.service.ChannelAuthService;
import com.dayan.channel.vo.AuthLoginVO;
import com.dayan.common.core.exception.AccountLockedException;
import com.dayan.common.core.exception.BusinessException;
import com.dayan.common.core.exception.ErrorCode;
import com.dayan.common.security.AccountType;
import com.dayan.common.security.StpKit;
import com.dayan.common.security.password.PasswordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Channel 渠道端认证服务实现。
 *
 * <p>登录流程：
 * <ol>
 *   <li>按 username/phone/email 任一查询 channel_account</li>
 *   <li>校验账号状态（锁定/禁用）</li>
 *   <li>BCrypt 校验密码</li>
 *   <li>Sa-Token login（loginType=channel，独立命名空间）</li>
 *   <li>更新登录时间/次数</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChannelAuthServiceImpl implements ChannelAuthService {

    private final ChannelAccountMapper accountMapper;
    private final PasswordService passwordService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AuthLoginVO login(AuthLoginDTO dto) {
        // 1. 按 username/phone/email 查询账号
        ChannelAccount account = accountMapper.selectOne(new LambdaQueryWrapper<ChannelAccount>()
                .eq(ChannelAccount::getUsername, dto.getUsername())
                .or()
                .eq(ChannelAccount::getPhone, dto.getUsername())
                .or()
                .eq(ChannelAccount::getEmail, dto.getUsername())
                .last("LIMIT 1"));
        if (account == null) {
            throw new BusinessException(ErrorCode.BUSINESS, "账号或密码错误");
        }

        // 2. 校验账号状态
        if (account.getAccountStatus() != null && account.getAccountStatus() == 0) {
            throw new AccountLockedException("账号已被锁定，请联系管理员");
        }
        if (account.getAccountStatus() != null && account.getAccountStatus() == 2) {
            throw new BusinessException(ErrorCode.BUSINESS, "账号已被禁用");
        }

        // 3. BCrypt 校验密码
        if (!passwordService.matches(dto.getPassword(), account.getPassword())) {
            throw new BusinessException(ErrorCode.BUSINESS, "账号或密码错误");
        }

        // 4. Sa-Token 登录（channel 命名空间）
        StpLogic logic = StpKit.CHANNEL;
        logic.login(account.getAccountCode());
        logic.getSession().set("channelCode", account.getChannelCode());
        logic.getSession().set("accountType", AccountType.CHANNEL.getLoginType());

        // 5. 更新登录信息
        ChannelAccount update = new ChannelAccount();
        update.setId(account.getId());
        update.setLoginCount((account.getLoginCount() == null ? 0 : account.getLoginCount()) + 1);
        update.setLastLoginTime(LocalDateTime.now());
        accountMapper.updateById(update);

        log.info("Channel 登录成功: accountCode={}, channelCode={}, username={}",
                account.getAccountCode(), account.getChannelCode(), account.getUsername());

        return AuthLoginVO.builder()
                .token(logic.getTokenValue())
                .tokenName(AccountType.CHANNEL.getTokenName())
                .accountCode(account.getAccountCode())
                .realName(account.getRealName())
                .avatar(account.getAvatar())
                .channelCode(account.getChannelCode())
                .build();
    }

    @Override
    public void logout() {
        StpKit.CHANNEL.logout();
    }

    @Override
    public AuthLoginVO current() {
        StpLogic logic = StpKit.CHANNEL;
        Object loginId = logic.getLoginIdDefaultNull();
        if (loginId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        ChannelAccount account = accountMapper.selectOne(new LambdaQueryWrapper<ChannelAccount>()
                .eq(ChannelAccount::getAccountCode, loginId.toString())
                .last("LIMIT 1"));
        if (account == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "账号不存在");
        }
        return AuthLoginVO.builder()
                .token(logic.getTokenValue())
                .tokenName(AccountType.CHANNEL.getTokenName())
                .accountCode(account.getAccountCode())
                .realName(account.getRealName())
                .avatar(account.getAvatar())
                .channelCode(account.getChannelCode())
                .build();
    }
}
