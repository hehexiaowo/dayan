package com.dayan.agent.service.impl;

import cn.dev33.satoken.stp.StpLogic;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dayan.agent.dto.AgentLoginDTO;
import com.dayan.agent.dto.SmsLoginDTO;
import com.dayan.agent.entity.AgentAccount;
import com.dayan.agent.entity.AgentInfo;
import com.dayan.agent.mapper.AgentAccountMapper;
import com.dayan.agent.mapper.AgentInfoMapper;
import com.dayan.agent.service.AgentAuthService;
import com.dayan.agent.service.AgentSmsCodeService;
import com.dayan.agent.service.WeChatLoginService;
import com.dayan.agent.vo.AgentLoginVO;
import com.dayan.agent.vo.ChannelOptionVO;
import com.dayan.channel.entity.ChannelInfo;
import com.dayan.channel.mapper.ChannelInfoMapper;
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
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Agent 代理人端认证服务实现。
 *
 * <p>登录流程：
 * <ol>
 *   <li>按 channelCode + (phone/open_id/username) 查询 agent_account</li>
 *   <li>校验账号状态（锁定/禁用）</li>
 *   <li>密码登录：BCrypt 校验密码；验证码登录：校验 Redis 验证码</li>
 *   <li>Sa-Token login（loginType=agent，独立命名空间）</li>
 *   <li>Session 存入 channelCode</li>
 *   <li>更新登录时间</li>
 * </ol>
 *
 * <p>选渠道特性：{@link #listChannels} 按 mobile/openId/username 查询并按 channel_code 去重返回渠道列表。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgentAuthServiceImpl implements AgentAuthService {

    private final AgentAccountMapper accountMapper;
    private final AgentInfoMapper agentInfoMapper;
    private final ChannelInfoMapper channelInfoMapper;
    private final PasswordService passwordService;
    private final AgentSmsCodeService smsCodeService;
    private final WeChatLoginService weChatLoginService;

    @Override
    public List<ChannelOptionVO> listChannels(String mobile, String openId) {
        if ((mobile == null || mobile.isBlank()) && (openId == null || openId.isBlank())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "手机号和 OpenID 至少传一个");
        }
        LambdaQueryWrapper<AgentAccount> wrapper = new LambdaQueryWrapper<AgentAccount>()
                .select(AgentAccount::getChannelCode);
        wrapper.and(w -> {
            if (mobile != null && !mobile.isBlank()) {
                // mobile 参数同时匹配 phone 和 username，使密码 Tab 输入用户名也能查到渠道
                w.eq(AgentAccount::getPhone, mobile)
                        .or().eq(AgentAccount::getUsername, mobile).or();
            }
            if (openId != null && !openId.isBlank()) {
                w.eq(AgentAccount::getOpenId, openId);
            }
        });
        List<AgentAccount> accounts = accountMapper.selectList(wrapper);
        List<String> codes = accounts.stream()
                .map(AgentAccount::getChannelCode)
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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AgentLoginVO login(AgentLoginDTO dto) {
        AgentAccount account = findByIdentifier(dto.getChannelCode(), dto.getIdentifier());
        verifyPassword(account, dto.getPassword());
        return doLogin(account);
    }

    @Override
    public AgentLoginVO smsLogin(SmsLoginDTO dto) {
        // 1. 校验验证码
        if (!smsCodeService.verifyAndConsume(dto.getMobile(), dto.getCode())) {
            throw new BusinessException(ErrorCode.BUSINESS, "验证码错误或已过期");
        }
        // 2. 查账号
        AgentAccount account = accountMapper.selectOne(new LambdaQueryWrapper<AgentAccount>()
                .eq(AgentAccount::getChannelCode, dto.getChannelCode())
                .eq(AgentAccount::getPhone, dto.getMobile())
                .last("LIMIT 1"));
        if (account == null) {
            throw new BusinessException(ErrorCode.BUSINESS, "账号不存在");
        }
        checkAccountStatus(account);
        return doLogin(account);
    }

    @Override
    public AgentLoginVO wxLogin(String code, String channelCode) {
        // 1. code → openId（骨架实现会抛异常）
        String openId = weChatLoginService.code2Session(code, channelCode);
        // 2. 按 openId + channelCode 查账号
        AgentAccount account = accountMapper.selectOne(new LambdaQueryWrapper<AgentAccount>()
                .eq(AgentAccount::getChannelCode, channelCode)
                .eq(AgentAccount::getOpenId, openId)
                .last("LIMIT 1"));
        if (account == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "该微信未绑定代理人账号");
        }
        checkAccountStatus(account);
        return doLogin(account);
    }

    // ==================== 内部方法 ====================

    /**
     * 按 channelCode + identifier（phone / open_id / username）查账号。
     */
    private AgentAccount findByIdentifier(String channelCode, String identifier) {
        LambdaQueryWrapper<AgentAccount> wrapper = new LambdaQueryWrapper<AgentAccount>()
                .eq(AgentAccount::getChannelCode, channelCode);
        wrapper.and(w -> w.eq(AgentAccount::getPhone, identifier)
                .or().eq(AgentAccount::getOpenId, identifier)
                .or().eq(AgentAccount::getUsername, identifier))
                .last("LIMIT 1");
        return accountMapper.selectOne(wrapper);
    }

    private void verifyPassword(AgentAccount account, String rawPassword) {
        if (account == null) {
            throw new BusinessException(ErrorCode.BUSINESS, "账号或密码错误");
        }
        checkAccountStatus(account);
        if (!passwordService.matches(rawPassword, account.getPassword())) {
            throw new BusinessException(ErrorCode.BUSINESS, "账号或密码错误");
        }
    }

    private void checkAccountStatus(AgentAccount account) {
        if (account.getAccountStatus() != null && account.getAccountStatus() == 0) {
            throw new AccountLockedException("账号已被锁定，请联系管理员");
        }
        if (account.getAccountStatus() != null && account.getAccountStatus() == 2) {
            throw new BusinessException(ErrorCode.BUSINESS, "账号已被禁用");
        }
    }

    private AgentLoginVO doLogin(AgentAccount account) {
        StpLogic logic = StpKit.AGENT;
        logic.login(account.getAgentCode());
        logic.getSession().set("channelCode", account.getChannelCode());
        logic.getSession().set("agentCode", account.getAgentCode());
        logic.getSession().set("accountType", AccountType.AGENT.getLoginType());
        logic.getSession().set("accountName", account.getUsername());

        AgentAccount update = new AgentAccount();
        update.setId(account.getId());
        update.setLastLoginTime(LocalDateTime.now());
        accountMapper.updateById(update);

        log.info("Agent 登录成功: agentCode={}, channelCode={}",
                account.getAgentCode(), account.getChannelCode());

        return withInfo(AgentLoginVO.builder()
                        .token(logic.getTokenValue())
                        .tokenName(AccountType.AGENT.getTokenName())
                        .agentCode(account.getAgentCode())
                        .channelCode(account.getChannelCode()),
                account)
                .build();
    }

    /** 联查 agent_info 回填姓名/头像（登录与 /auth/info 共用；无资料记录则留 null） */
    private AgentLoginVO.AgentLoginVOBuilder withInfo(AgentLoginVO.AgentLoginVOBuilder builder,
                                                      AgentAccount account) {
        AgentInfo info = agentInfoMapper.selectOne(new LambdaQueryWrapper<AgentInfo>()
                .eq(AgentInfo::getChannelCode, account.getChannelCode())
                .eq(AgentInfo::getAgentCode, account.getAgentCode())
                .last("LIMIT 1"));
        if (info != null) {
            builder.realName(info.getFullName()).avatar(info.getAvatar());
        }
        return builder;
    }

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
    public void logout() {
        StpKit.AGENT.logout();
    }

    @Override
    public AgentLoginVO current() {
        StpLogic logic = StpKit.AGENT;
        Object loginId = logic.getLoginIdDefaultNull();
        if (loginId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        AgentAccount account = accountMapper.selectOne(new LambdaQueryWrapper<AgentAccount>()
                .eq(AgentAccount::getAgentCode, loginId.toString())
                .last("LIMIT 1"));
        if (account == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "账号不存在");
        }
        return withInfo(AgentLoginVO.builder()
                        .token(logic.getTokenValue())
                        .tokenName(AccountType.AGENT.getTokenName())
                        .agentCode(account.getAgentCode())
                        .channelCode(account.getChannelCode()),
                account)
                .build();
    }
}
