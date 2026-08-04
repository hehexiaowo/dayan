package com.dayan.agent.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dayan.agent.dto.AgentAccountCreateDTO;
import com.dayan.agent.dto.AgentAccountQueryDTO;
import com.dayan.agent.dto.AgentAccountUpdateDTO;
import com.dayan.agent.entity.AgentAccount;
import com.dayan.agent.entity.AgentInfo;
import com.dayan.agent.mapper.AgentAccountMapper;
import com.dayan.agent.mapper.AgentInfoMapper;
import com.dayan.agent.service.AgentAccountService;
import com.dayan.agent.vo.AgentAccountVO;
import com.dayan.common.core.exception.BusinessException;
import com.dayan.common.core.exception.ErrorCode;
import com.dayan.common.core.resp.PageResult;
import com.dayan.common.mybatis.context.ContextHolder;
import com.dayan.common.security.password.PasswordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 代理人账号（agent_account）服务实现。
 *
 * <p>复用 {@link PasswordService} 进行 BCrypt 哈希；
 * 渠道隔离由 {@code TenantLineInnerInterceptor} 自动追加 channel_code 条件。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgentAccountServiceImpl implements AgentAccountService {

    /** 重置密码默认值 */
    private static final String DEFAULT_RESET_PASSWORD = "dayan@123";

    private final AgentAccountMapper accountMapper;
    private final AgentInfoMapper agentInfoMapper;
    private final PasswordService passwordService;

    @Override
    public PageResult<AgentAccountVO> page(AgentAccountQueryDTO query) {
        LambdaQueryWrapper<AgentAccount> wrapper = new LambdaQueryWrapper<AgentAccount>()
                .orderByDesc(AgentAccount::getCreatedAt);
        if (query.getChannelCode() != null && !query.getChannelCode().isEmpty()) {
            wrapper.eq(AgentAccount::getChannelCode, query.getChannelCode());
        }
        if (query.getAgentCode() != null && !query.getAgentCode().isEmpty()) {
            wrapper.eq(AgentAccount::getAgentCode, query.getAgentCode());
        }
        if (query.getKeyword() != null && !query.getKeyword().isEmpty()) {
            wrapper.and(w -> w.like(AgentAccount::getUsername, query.getKeyword())
                    .or().like(AgentAccount::getPhone, query.getKeyword()));
        }
        if (query.getAccountStatus() != null) {
            wrapper.eq(AgentAccount::getAccountStatus, query.getAccountStatus());
        }
        Page<AgentAccount> page = accountMapper.selectPage(
                new Page<>(query.getCurrent(), query.getSize()), wrapper);
        List<AgentAccountVO> records = page.getRecords().stream().map(this::toVO).toList();
        return new PageResult<>(query.getCurrent(), query.getSize(), page.getTotal(), records);
    }

    @Override
    public AgentAccountVO getDetail(String agentCode) {
        return toVO(selectByAgentCode(agentCode));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String create(AgentAccountCreateDTO dto) {
        String channelCode = pickChannelCode(dto.getChannelCode());

        // 校验代理人存在（渠道内）
        AgentInfo agent = agentInfoMapper.selectOne(new LambdaQueryWrapper<AgentInfo>()
                .eq(AgentInfo::getChannelCode, channelCode)
                .eq(AgentInfo::getAgentCode, dto.getAgentCode())
                .last("LIMIT 1"));
        if (agent == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "代理人不存在: " + dto.getAgentCode());
        }

        // 同渠道 1:1 校验：一个 agent_code 仅能有一个账号
        Long existCount = accountMapper.selectCount(new LambdaQueryWrapper<AgentAccount>()
                .eq(AgentAccount::getChannelCode, channelCode)
                .eq(AgentAccount::getAgentCode, dto.getAgentCode()));
        if (existCount > 0) {
            throw new BusinessException(ErrorCode.BUSINESS, "该代理人已存在账号: " + dto.getAgentCode());
        }

        // username 渠道内唯一校验（若提供）
        if (dto.getUsername() != null && !dto.getUsername().isEmpty()) {
            Long usernameCount = accountMapper.selectCount(new LambdaQueryWrapper<AgentAccount>()
                    .eq(AgentAccount::getChannelCode, channelCode)
                    .eq(AgentAccount::getUsername, dto.getUsername()));
            if (usernameCount > 0) {
                throw new BusinessException(ErrorCode.BUSINESS, "用户名已存在: " + dto.getUsername());
            }
        }

        AgentAccount entity = new AgentAccount();
        entity.setAgentCode(dto.getAgentCode());
        entity.setChannelCode(channelCode);
        entity.setUsername(dto.getUsername());
        entity.setPhone(dto.getPhone());
        entity.setOpenId(dto.getOpenId());
        entity.setUnionId(dto.getUnionId());
        entity.setExtAccountNo(dto.getExtAccountNo());
        String rawPwd = (dto.getPassword() == null || dto.getPassword().isEmpty())
                ? DEFAULT_RESET_PASSWORD : dto.getPassword();
        entity.setPassword(passwordService.encode(rawPwd));
        entity.setSalt("bcrypt");
        entity.setAccountStatus(dto.getAccountStatus() == null ? 1 : dto.getAccountStatus());
        accountMapper.insert(entity);
        log.info("创建代理人账号成功: agentCode={}, channelCode={}", entity.getAgentCode(), channelCode);
        return entity.getAgentCode();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(String agentCode, AgentAccountUpdateDTO dto) {
        AgentAccount existing = selectByAgentCode(agentCode);
        // username 变更时做渠道内唯一校验
        if (dto.getUsername() != null && !dto.getUsername().isEmpty()
                && !dto.getUsername().equals(existing.getUsername())) {
            Long usernameCount = accountMapper.selectCount(new LambdaQueryWrapper<AgentAccount>()
                    .eq(AgentAccount::getChannelCode, existing.getChannelCode())
                    .eq(AgentAccount::getUsername, dto.getUsername())
                    .ne(AgentAccount::getId, existing.getId()));
            if (usernameCount > 0) {
                throw new BusinessException(ErrorCode.BUSINESS, "用户名已存在: " + dto.getUsername());
            }
        }
        AgentAccount update = new AgentAccount();
        update.setId(existing.getId());
        if (dto.getUsername() != null) update.setUsername(dto.getUsername());
        if (dto.getPhone() != null) update.setPhone(dto.getPhone());
        if (dto.getOpenId() != null) update.setOpenId(dto.getOpenId());
        if (dto.getUnionId() != null) update.setUnionId(dto.getUnionId());
        if (dto.getExtAccountNo() != null) update.setExtAccountNo(dto.getExtAccountNo());
        if (dto.getAccountStatus() != null) update.setAccountStatus(dto.getAccountStatus());
        accountMapper.updateById(update);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resetPassword(String agentCode) {
        AgentAccount existing = selectByAgentCode(agentCode);
        AgentAccount update = new AgentAccount();
        update.setId(existing.getId());
        update.setPassword(passwordService.encode(DEFAULT_RESET_PASSWORD));
        accountMapper.updateById(update);
        log.info("重置代理人账号密码: agentCode={}", agentCode);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(String agentCode) {
        AgentAccount existing = selectByAgentCode(agentCode);
        accountMapper.deleteById(existing.getId());
    }

    private AgentAccount selectByAgentCode(String agentCode) {
        AgentAccount account = accountMapper.selectOne(new LambdaQueryWrapper<AgentAccount>()
                .eq(AgentAccount::getAgentCode, agentCode).last("LIMIT 1"));
        if (account == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "代理人账号不存在: " + agentCode);
        }
        return account;
    }

    private String pickChannelCode(String fromDto) {
        if (fromDto != null && !fromDto.isEmpty()) {
            return fromDto;
        }
        String ctx = ContextHolder.getChannelCode();
        if (ctx == null || ctx.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "渠道编码不能为空");
        }
        return ctx;
    }

    private AgentAccountVO toVO(AgentAccount entity) {
        AgentAccountVO vo = new AgentAccountVO();
        vo.setId(entity.getId());
        vo.setAgentCode(entity.getAgentCode());
        vo.setChannelCode(entity.getChannelCode());
        vo.setUsername(entity.getUsername());
        vo.setPhone(entity.getPhone());
        vo.setOpenId(entity.getOpenId());
        vo.setUnionId(entity.getUnionId());
        vo.setExtAccountNo(entity.getExtAccountNo());
        vo.setAccountStatus(entity.getAccountStatus());
        vo.setLastLoginTime(entity.getLastLoginTime());
        vo.setLastLoginIp(entity.getLastLoginIp());
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }
}
