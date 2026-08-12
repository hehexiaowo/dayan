package com.dayan.client.service.impl;

import cn.dev33.satoken.stp.StpLogic;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dayan.client.dto.ClientLoginDTO;
import com.dayan.client.entity.ClientAccount;
import com.dayan.client.mapper.ClientAccountMapper;
import com.dayan.client.service.ClientAuthService;
import com.dayan.client.vo.ChannelOptionVO;
import com.dayan.client.vo.ClientLoginVO;
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
import java.util.stream.Collectors;

/**
 * Client 客户端认证服务实现。
 *
 * <p>登录流程：
 * <ol>
 *   <li>按 channelCode + (phone 或 open_id) 查询 client_account</li>
 *   <li>校验账号状态（锁定/禁用）</li>
 *   <li>BCrypt 校验密码</li>
 *   <li>Sa-Token login（loginType=client，独立命名空间）</li>
 *   <li>Session 存入 channelCode</li>
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
    private final PasswordService passwordService;

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
        return accounts.stream()
                .map(ClientAccount::getChannelCode)
                .filter(code -> code != null && !code.isBlank())
                .distinct()
                .map(code -> ChannelOptionVO.builder().channelCode(code).build())
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ClientLoginVO login(ClientLoginDTO dto) {
        // 1. 按 channelCode + (phone 或 open_id) 查询账号
        LambdaQueryWrapper<ClientAccount> wrapper = new LambdaQueryWrapper<ClientAccount>()
                .eq(ClientAccount::getChannelCode, dto.getChannelCode());
        wrapper.and(w -> w.eq(ClientAccount::getPhone, dto.getIdentifier())
                .or()
                .eq(ClientAccount::getOpenId, dto.getIdentifier()))
                .last("LIMIT 1");
        ClientAccount account = accountMapper.selectOne(wrapper);
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

        // 4. Sa-Token 登录（client 命名空间）
        StpLogic logic = StpKit.CLIENT;
        logic.login(account.getClientCode());
        logic.getSession().set("channelCode", account.getChannelCode());
        logic.getSession().set("clientCode", account.getClientCode());
        logic.getSession().set("accountType", AccountType.CLIENT.getLoginType());
        // 客户全名快照：优先 username，为空则用 clientCode 兜底
        // （SaSession 基于 ConcurrentHashMap 不允许 null value；且 equity_activate.client_full_name 为 NOT NULL）。
        String clientFullName = account.getUsername() != null ? account.getUsername() : account.getClientCode();
        logic.getSession().set("clientFullName", clientFullName);

        // 5. 更新登录时间/次数
        ClientAccount update = new ClientAccount();
        update.setId(account.getId());
        update.setLoginCount((account.getLoginCount() == null ? 0 : account.getLoginCount()) + 1);
        update.setLastLoginTime(LocalDateTime.now());
        accountMapper.updateById(update);

        log.info("Client 登录成功: clientCode={}, channelCode={}",
                account.getClientCode(), account.getChannelCode());

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
