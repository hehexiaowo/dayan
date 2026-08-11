package com.dayan.agent.service.impl;

import cn.dev33.satoken.stp.StpLogic;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dayan.agent.dto.AgentLoginDTO;
import com.dayan.agent.entity.AgentAccount;
import com.dayan.agent.mapper.AgentAccountMapper;
import com.dayan.agent.service.AgentAuthService;
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
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Agent 代理人端认证服务实现。
 *
 * <p>登录流程：
 * <ol>
 *   <li>按 channelCode + (phone 或 open_id) 查询 agent_account</li>
 *   <li>校验账号状态（锁定/禁用）</li>
 *   <li>BCrypt 校验密码</li>
 *   <li>Sa-Token login（loginType=agent，独立命名空间）</li>
 *   <li>Session 存入 channelCode</li>
 *   <li>更新登录时间</li>
 * </ol>
 *
 * <p>选渠道特性：{@link #listChannels} 按 mobile/openId 查询并按 channel_code 去重返回渠道列表。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgentAuthServiceImpl implements AgentAuthService {

    private final AgentAccountMapper accountMapper;
    private final ChannelInfoMapper channelInfoMapper;
    private final PasswordService passwordService;

    @Override
    public List<ChannelOptionVO> listChannels(String mobile, String openId) {
        if ((mobile == null || mobile.isBlank()) && (openId == null || openId.isBlank())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "手机号和 OpenID 至少传一个");
        }
        LambdaQueryWrapper<AgentAccount> wrapper = new LambdaQueryWrapper<AgentAccount>()
                .select(AgentAccount::getChannelCode);
        // mobile 或 openId 命中即返回
        wrapper.and(w -> {
            if (mobile != null && !mobile.isBlank()) {
                w.eq(AgentAccount::getPhone, mobile).or();
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
        // 批量联查 channel_info 回填简称/全称，避免 N+1
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

    /**
     * 按 channelCode 集合一次性查 channel_info，组装 Map 便于回填。
     */
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
    public AgentLoginVO login(AgentLoginDTO dto) {
        // 1. 按 channelCode + (phone 或 open_id) 查询账号
        LambdaQueryWrapper<AgentAccount> wrapper = new LambdaQueryWrapper<AgentAccount>()
                .eq(AgentAccount::getChannelCode, dto.getChannelCode());
        wrapper.and(w -> w.eq(AgentAccount::getPhone, dto.getIdentifier())
                .or()
                .eq(AgentAccount::getOpenId, dto.getIdentifier()))
                .last("LIMIT 1");
        AgentAccount account = accountMapper.selectOne(wrapper);
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

        // 4. Sa-Token 登录（agent 命名空间）
        StpLogic logic = StpKit.AGENT;
        logic.login(account.getAgentCode());
        logic.getSession().set("channelCode", account.getChannelCode());
        logic.getSession().set("agentCode", account.getAgentCode());
        logic.getSession().set("accountType", AccountType.AGENT.getLoginType());
        // AgentAccount 无 realName 字段，用 username 作为操作人姓名落库审计
        logic.getSession().set("accountName", account.getUsername());

        // 5. 更新登录时间
        AgentAccount update = new AgentAccount();
        update.setId(account.getId());
        update.setLastLoginTime(LocalDateTime.now());
        accountMapper.updateById(update);

        log.info("Agent 登录成功: agentCode={}, channelCode={}",
                account.getAgentCode(), account.getChannelCode());

        return AgentLoginVO.builder()
                .token(logic.getTokenValue())
                .tokenName(AccountType.AGENT.getTokenName())
                .agentCode(account.getAgentCode())
                .channelCode(account.getChannelCode())
                .build();
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
        return AgentLoginVO.builder()
                .token(logic.getTokenValue())
                .tokenName(AccountType.AGENT.getTokenName())
                .agentCode(account.getAgentCode())
                .channelCode(account.getChannelCode())
                .build();
    }
}
