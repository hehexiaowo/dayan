package com.dayan.client.service.impl;

import cn.dev33.satoken.stp.StpLogic;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dayan.client.dto.ClientLoginDTO;
import com.dayan.client.dto.SmsLoginDTO;
import com.dayan.client.entity.ClientAccount;
import com.dayan.client.mapper.ClientAccountMapper;
import com.dayan.channel.entity.ChannelInfo;
import com.dayan.channel.mapper.ChannelInfoMapper;
import com.dayan.client.service.ClientAuthService;
import com.dayan.client.service.ClientSmsCodeService;
import com.dayan.client.service.ClientWeChatLoginService;
import com.dayan.client.vo.ChannelOptionVO;
import com.dayan.client.vo.ClientLoginVO;
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
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Client 客户端认证服务实现。
 *
 * <p>登录流程：
 * <ol>
 *   <li>按 channelCode + (phone/open_id) 查询 client_account</li>
 *   <li>校验账号状态（锁定/禁用）</li>
 *   <li>密码登录：BCrypt 校验密码；验证码登录：校验 Redis 验证码</li>
 *   <li>Sa-Token login（loginType=client，独立命名空间）</li>
 *   <li>Session 存入 channelCode/clientCode/clientFullName</li>
 *   <li>更新登录时间/次数</li>
 * </ol>
 *
 * <p>选渠道特性：{@link #listChannels} 按 mobile/openId 查询并按 channel_code 去重返回渠道列表。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ClientAuthServiceImpl implements ClientAuthService {

    private final ClientAccountMapper accountMapper;
    private final ChannelInfoMapper channelInfoMapper;
    private final PasswordService passwordService;
    private final ClientSmsCodeService smsCodeService;
    private final ClientWeChatLoginService weChatLoginService;
    private final ObjectProvider<AuthLogRecorder> authLogRecorderProvider;

    @Override
    public List<ChannelOptionVO> listChannels(String mobile, String openId) {
        if ((mobile == null || mobile.isBlank()) && (openId == null || openId.isBlank())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "手机号和 OpenID 至少传一个");
        }
        LambdaQueryWrapper<ClientAccount> wrapper = new LambdaQueryWrapper<ClientAccount>()
                .select(ClientAccount::getChannelCode);
        // mobile 或 openId 命中即返回
        wrapper.and(w -> {
            if (mobile != null && !mobile.isBlank()) {
                w.eq(ClientAccount::getPhone, mobile).or();
            }
            if (openId != null && !openId.isBlank()) {
                w.eq(ClientAccount::getOpenId, openId);
            }
        });
        List<ClientAccount> accounts = accountMapper.selectList(wrapper);
        List<String> codes = accounts.stream()
                .map(ClientAccount::getChannelCode)
                .filter(code -> code != null && !code.isBlank())
                .distinct()
                .collect(Collectors.toList());
        if (codes.isEmpty()) {
            return List.of();
        }
        Map<String, ChannelInfo> channelMap = resolveChannelNames(codes);
        return codes.stream()
                .map(code -> {
                    ChannelInfo ch = channelMap.get(code);
                    return ChannelOptionVO.builder()
                            .channelCode(code)
                            .shortName(ch != null ? ch.getShortName() : null)
                            .fullName(ch != null ? ch.getFullName() : null)
                            .build();
                })
                .collect(Collectors.toList());
    }

    /** 批量查 channel_info 的 shortName/fullName（登录选渠道展示「简称（编码）」） */
    private Map<String, ChannelInfo> resolveChannelNames(List<String> channelCodes) {
        List<ChannelInfo> channels = channelInfoMapper.selectList(
                new LambdaQueryWrapper<ChannelInfo>()
                        .in(ChannelInfo::getChannelCode, channelCodes)
                        .select(ChannelInfo::getChannelCode,
                                ChannelInfo::getShortName,
                                ChannelInfo::getFullName));
        return channels.stream()
                .collect(Collectors.toMap(ChannelInfo::getChannelCode, ch -> ch, (a, b) -> a));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ClientLoginVO login(ClientLoginDTO dto) {
        ClientAccount account = findByIdentifier(dto.getChannelCode(), dto.getIdentifier());
        if (account == null) {
            recordLogin(null, null, dto.getIdentifier(), "password", false, "账号或密码错误");
            throw new BusinessException(ErrorCode.BUSINESS, "账号或密码错误");
        }
        checkAccountStatus(account, dto.getIdentifier(), "password");
        if (!passwordService.matches(dto.getPassword(), account.getPassword())) {
            recordLogin(account.getClientCode(), account.getUsername(), dto.getIdentifier(), "password", false, "账号或密码错误");
            throw new BusinessException(ErrorCode.BUSINESS, "账号或密码错误");
        }
        return doLogin(account, "password", dto.getIdentifier());
    }

    @Override
    public ClientLoginVO smsLogin(SmsLoginDTO dto) {
        // 1. 校验验证码
        if (!smsCodeService.verifyAndConsume(dto.getMobile(), dto.getCode())) {
            recordLogin(null, null, dto.getMobile(), "sms", false, "验证码错误或已过期");
            throw new BusinessException(ErrorCode.BUSINESS, "验证码错误或已过期");
        }
        // 2. 查账号
        ClientAccount account = accountMapper.selectOne(new LambdaQueryWrapper<ClientAccount>()
                .eq(ClientAccount::getChannelCode, dto.getChannelCode())
                .eq(ClientAccount::getPhone, dto.getMobile())
                .last("LIMIT 1"));
        if (account == null) {
            recordLogin(null, null, dto.getMobile(), "sms", false, "账号不存在");
            throw new BusinessException(ErrorCode.BUSINESS, "账号不存在");
        }
        checkAccountStatus(account, dto.getMobile(), "sms");
        return doLogin(account, "sms", dto.getMobile());
    }

    @Override
    public ClientLoginVO wxLogin(String code, String channelCode) {
        // 1. code → openId（骨架实现会抛异常）
        String openId = weChatLoginService.code2Session(code, channelCode);
        // 2. 按 openId + channelCode 查账号
        ClientAccount account = accountMapper.selectOne(new LambdaQueryWrapper<ClientAccount>()
                .eq(ClientAccount::getChannelCode, channelCode)
                .eq(ClientAccount::getOpenId, openId)
                .last("LIMIT 1"));
        if (account == null) {
            recordLogin(null, null, openId, "wx", false, "该微信未绑定客户账号");
            throw new BusinessException(ErrorCode.NOT_FOUND, "该微信未绑定客户账号");
        }
        checkAccountStatus(account, openId, "wx");
        return doLogin(account, "wx", openId);
    }

    // ==================== 内部方法 ====================

    /**
     * 按 channelCode + identifier（phone / open_id）查账号。
     */
    private ClientAccount findByIdentifier(String channelCode, String identifier) {
        LambdaQueryWrapper<ClientAccount> wrapper = new LambdaQueryWrapper<ClientAccount>()
                .eq(ClientAccount::getChannelCode, channelCode);
        wrapper.and(w -> w.eq(ClientAccount::getPhone, identifier)
                .or()
                .eq(ClientAccount::getOpenId, identifier))
                .last("LIMIT 1");
        return accountMapper.selectOne(wrapper);
    }

    private void checkAccountStatus(ClientAccount account, String identity, String loginType) {
        if (account.getAccountStatus() != null && account.getAccountStatus() == 0) {
            recordLogin(account.getClientCode(), account.getUsername(), identity, loginType, false, "账号已被锁定");
            throw new AccountLockedException("账号已被锁定，请联系管理员");
        }
        if (account.getAccountStatus() != null && account.getAccountStatus() == 2) {
            recordLogin(account.getClientCode(), account.getUsername(), identity, loginType, false, "账号已被禁用");
            throw new BusinessException(ErrorCode.BUSINESS, "账号已被禁用");
        }
    }

    /** 登录日志（成功/失败），经 SPI 落库到 system_log_client；无实现时静默跳过 */
    private void recordLogin(String accountCode, String accountName, String identity, String loginType,
                             boolean success, String failReason) {
        AuthLogRecorder recorder = authLogRecorderProvider.getIfAvailable();
        if (recorder != null) {
            recorder.recordLogin(AccountType.CLIENT.getLoginType(), accountCode, accountName,
                    loginType, identity, success, failReason);
        }
    }

    /**
     * 公共登录收尾：Sa-Token 登录 + Session 快照 + 更新登录时间/次数 + 构造返回 VO。
     * 密码 / 验证码 / 微信三种登录方式共用。
     */
    private ClientLoginVO doLogin(ClientAccount account, String loginType, String identity) {
        StpLogic logic = StpKit.CLIENT;
        logic.login(account.getClientCode());
        logic.getSession().set("channelCode", account.getChannelCode());
        logic.getSession().set("clientCode", account.getClientCode());
        logic.getSession().set("accountType", AccountType.CLIENT.getLoginType());
        // 客户全名快照：优先 username，为空则用 clientCode 兜底
        // （SaSession 基于 ConcurrentHashMap 不允许 null value；且 equity_activate.client_full_name 为 NOT NULL）。
        String clientFullName = account.getUsername() != null ? account.getUsername() : account.getClientCode();
        logic.getSession().set("clientFullName", clientFullName);

        // 更新登录时间/次数
        ClientAccount update = new ClientAccount();
        update.setId(account.getId());
        update.setLoginCount((account.getLoginCount() == null ? 0 : account.getLoginCount()) + 1);
        update.setLastLoginTime(LocalDateTime.now());
        accountMapper.updateById(update);

        log.info("Client 登录成功: clientCode={}, channelCode={}",
                account.getClientCode(), account.getChannelCode());
        recordLogin(account.getClientCode(), account.getUsername(), identity, loginType, true, null);

        return ClientLoginVO.builder()
                .token(logic.getTokenValue())
                .tokenName(AccountType.CLIENT.getTokenName())
                .clientCode(account.getClientCode())
                .clientName(account.getUsername())
                .channelCode(account.getChannelCode())
                .build();
    }

    @Override
    public void logout() {
        AuthLogRecorder recorder = authLogRecorderProvider.getIfAvailable();
        if (recorder != null) {
            recorder.recordLogout(AccountType.CLIENT.getLoginType(),
                    ContextHolder.getAccountCode(), ContextHolder.getAccountName());
        }
        StpKit.CLIENT.logout();
    }

    @Override
    public ClientLoginVO current() {
        StpLogic logic = StpKit.CLIENT;
        Object loginId = logic.getLoginIdDefaultNull();
        if (loginId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        ClientAccount account = accountMapper.selectOne(new LambdaQueryWrapper<ClientAccount>()
                .eq(ClientAccount::getClientCode, loginId.toString())
                .last("LIMIT 1"));
        if (account == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "账号不存在");
        }
        return ClientLoginVO.builder()
                .token(logic.getTokenValue())
                .tokenName(AccountType.CLIENT.getTokenName())
                .clientCode(account.getClientCode())
                .clientName(account.getUsername())
                .channelCode(account.getChannelCode())
                .build();
    }
}
