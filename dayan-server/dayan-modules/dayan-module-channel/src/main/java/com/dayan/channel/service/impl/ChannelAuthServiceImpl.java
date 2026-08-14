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
import com.dayan.common.log.auth.AuthLogRecorder;
import com.dayan.common.mybatis.context.ContextHolder;
import com.dayan.common.security.AccountType;
import com.dayan.common.security.StpKit;
import com.dayan.common.security.password.PasswordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
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
    private final ObjectProvider<AuthLogRecorder> authLogRecorderProvider;

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
            recordLogin(null, null, dto.getUsername(), false, "账号或密码错误");
            throw new BusinessException(ErrorCode.BUSINESS, "账号或密码错误");
        }

        // 关键：在写操作前把 channelCode 注入 ContextHolder。
        // 登录时尚无 Sa-Token 会话，SaTokenContextFilter 无法填充 channelCode，
        // 导致 DayanTenantHandler.getTenantId() 返回 LongValue(0L)，
        // TenantLineInnerInterceptor 追加 `AND channel_code = 0` 到 UPDATE，
        // MySQL 把 VARCHAR 'CH00001' 与 0 比较 → DOUBLE 强转截断 → MysqlDataTruncation。
        ContextHolder.setChannelCode(account.getChannelCode());
        ContextHolder.setAccountCode(account.getAccountCode());
        ContextHolder.setAccountType(AccountType.CHANNEL.getLoginType());
        ContextHolder.setAccountName(account.getRealName());

        // 2. 校验账号状态
        if (account.getAccountStatus() != null && account.getAccountStatus() == 0) {
            recordLogin(account.getAccountCode(), account.getRealName(), dto.getUsername(), false, "账号已被锁定");
            throw new AccountLockedException("账号已被锁定，请联系管理员");
        }
        if (account.getAccountStatus() != null && account.getAccountStatus() == 2) {
            recordLogin(account.getAccountCode(), account.getRealName(), dto.getUsername(), false, "账号已被禁用");
            throw new BusinessException(ErrorCode.BUSINESS, "账号已被禁用");
        }

        // 3. BCrypt 校验密码
        if (!passwordService.matches(dto.getPassword(), account.getPassword())) {
            recordLogin(account.getAccountCode(), account.getRealName(), dto.getUsername(), false, "账号或密码错误");
            throw new BusinessException(ErrorCode.BUSINESS, "账号或密码错误");
        }

        // 4. Sa-Token 登录（channel 命名空间）
        StpLogic logic = StpKit.CHANNEL;
        logic.login(account.getAccountCode());
        logic.getSession().set("channelCode", account.getChannelCode());
        logic.getSession().set("accountType", AccountType.CHANNEL.getLoginType());
        // 存入操作人姓名，供 SaTokenContextFilter 填充 ContextHolder、操作日志审计读取
        logic.getSession().set("accountName", account.getRealName());

        // 5. 更新登录信息
        ChannelAccount update = new ChannelAccount();
        update.setId(account.getId());
        update.setLoginCount((account.getLoginCount() == null ? 0 : account.getLoginCount()) + 1);
        update.setLastLoginTime(LocalDateTime.now());
        accountMapper.updateById(update);

        log.info("Channel 登录成功: accountCode={}, channelCode={}, username={}",
                account.getAccountCode(), account.getChannelCode(), account.getUsername());
        recordLogin(account.getAccountCode(), account.getRealName(), account.getUsername(), true, null);

        return AuthLoginVO.builder()
                .token(logic.getTokenValue())
                .tokenName(AccountType.CHANNEL.getTokenName())
                .accountCode(account.getAccountCode())
                .realName(account.getRealName())
                .avatar(account.getAvatar())
                .channelCode(account.getChannelCode())
                .isAdmin(account.getIsAdmin())
                .build();
    }

    @Override
    public void logout() {
        AuthLogRecorder recorder = authLogRecorderProvider.getIfAvailable();
        if (recorder != null) {
            recorder.recordLogout(AccountType.CHANNEL.getLoginType(),
                    ContextHolder.getAccountCode(), ContextHolder.getAccountName());
        }
        StpKit.CHANNEL.logout();
    }

    /** 登录日志（成功/失败），经 SPI 落库到 system_log_channel；无实现时静默跳过 */
    private void recordLogin(String accountCode, String accountName, String identity,
                             boolean success, String failReason) {
        AuthLogRecorder recorder = authLogRecorderProvider.getIfAvailable();
        if (recorder != null) {
            recorder.recordLogin(AccountType.CHANNEL.getLoginType(), accountCode, accountName,
                    "password", identity, success, failReason);
        }
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
                .isAdmin(account.getIsAdmin())
                .build();
    }
}
