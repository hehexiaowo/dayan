package com.dayan.agent.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dayan.agent.dto.AgentClientRelBindDTO;
import com.dayan.agent.dto.AgentClientRelQueryDTO;
import com.dayan.agent.entity.AgentClientRel;
import com.dayan.agent.entity.AgentInfo;
import com.dayan.agent.mapper.AgentClientRelMapper;
import com.dayan.agent.mapper.AgentInfoMapper;
import com.dayan.agent.service.AgentClientRelService;
import com.dayan.agent.vo.AgentClientRelVO;
import com.dayan.common.core.exception.BusinessException;
import com.dayan.common.core.exception.ErrorCode;
import com.dayan.common.core.resp.PageResult;
import com.dayan.common.mybatis.context.ContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 代理人-客户绑定关系（agent_client_rel）服务实现。
 *
 * <p>绑定校验：代理人存在、客户存在、同一 agent_code + client_code 当前无有效绑定（status=1）。
 *
 * <p>注：agent_client_rel 表无 channel_code 字段（非分片表），渠道隔离依赖
 * agent_code / client_code 本身的渠道归属；agent/client 存在性校验各自走分片表。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgentClientRelServiceImpl implements AgentClientRelService {

    private final AgentClientRelMapper relMapper;
    private final AgentInfoMapper agentInfoMapper;

    @Override
    public PageResult<AgentClientRelVO> page(AgentClientRelQueryDTO query) {
        LambdaQueryWrapper<AgentClientRel> wrapper = new LambdaQueryWrapper<AgentClientRel>()
                .orderByDesc(AgentClientRel::getBindTime);
        if (query.getAgentCode() != null && !query.getAgentCode().isEmpty()) {
            wrapper.eq(AgentClientRel::getAgentCode, query.getAgentCode());
        }
        if (query.getAgentCodes() != null && !query.getAgentCodes().isEmpty()) {
            wrapper.in(AgentClientRel::getAgentCode, query.getAgentCodes());
        }
        if (query.getClientCode() != null && !query.getClientCode().isEmpty()) {
            wrapper.eq(AgentClientRel::getClientCode, query.getClientCode());
        }
        if (query.getBindType() != null) {
            wrapper.eq(AgentClientRel::getBindType, query.getBindType());
        }
        if (query.getStatus() != null) {
            wrapper.eq(AgentClientRel::getStatus, query.getStatus());
        }
        Page<AgentClientRel> page = relMapper.selectPage(
                new Page<>(query.getCurrent(), query.getSize()), wrapper);
        List<AgentClientRelVO> records = page.getRecords().stream().map(this::toVO).toList();
        return new PageResult<>(query.getCurrent(), query.getSize(), page.getTotal(), records);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long bind(AgentClientRelBindDTO dto) {
        String channelCode = ContextHolder.getChannelCode();

        // 1. 校验代理人存在（渠道内）
        LambdaQueryWrapper<AgentInfo> agentWrapper = new LambdaQueryWrapper<AgentInfo>()
                .eq(AgentInfo::getAgentCode, dto.getAgentCode())
                .last("LIMIT 1");
        if (channelCode != null && !channelCode.isEmpty()) {
            agentWrapper.eq(AgentInfo::getChannelCode, channelCode);
        }
        AgentInfo agent = agentInfoMapper.selectOne(agentWrapper);
        if (agent == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "代理人不存在: " + dto.getAgentCode());
        }

        // 2. 校验客户存在（渠道内，跨域只读 client_info）
        if (relMapper.countClientByCode(dto.getClientCode()) <= 0) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "客户不存在: " + dto.getClientCode());
        }

        // 3. 校验同一 agent_code + client_code 当前无有效绑定
        Long activeCount = relMapper.selectCount(new LambdaQueryWrapper<AgentClientRel>()
                .eq(AgentClientRel::getAgentCode, dto.getAgentCode())
                .eq(AgentClientRel::getClientCode, dto.getClientCode())
                .eq(AgentClientRel::getStatus, 1));
        if (activeCount > 0) {
            throw new BusinessException(ErrorCode.BUSINESS,
                    "该代理人已绑定此客户，且绑定仍在有效期内");
        }

        AgentClientRel entity = new AgentClientRel();
        entity.setAgentCode(dto.getAgentCode());
        entity.setClientCode(dto.getClientCode());
        entity.setBindType(dto.getBindType() == null ? 1 : dto.getBindType());
        entity.setBindTime(LocalDateTime.now());
        entity.setStatus(1);
        relMapper.insert(entity);
        log.info("绑定代理人-客户: agentCode={}, clientCode={}",
                dto.getAgentCode(), dto.getClientCode());
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unbind(Long id) {
        AgentClientRel existing = relMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "绑定关系不存在: id=" + id);
        }
        AgentClientRel update = new AgentClientRel();
        update.setId(id);
        update.setStatus(0);
        relMapper.updateById(update);
        log.info("解绑代理人-客户: id={}, agentCode={}, clientCode={}",
                id, existing.getAgentCode(), existing.getClientCode());
    }

    @Override
    public List<AgentClientRelVO> listByAgent(String agentCode) {
        List<AgentClientRel> list = relMapper.selectList(new LambdaQueryWrapper<AgentClientRel>()
                .eq(AgentClientRel::getAgentCode, agentCode)
                .orderByDesc(AgentClientRel::getBindTime));
        return list.stream().map(this::toVO).toList();
    }

    @Override
    public List<AgentClientRelVO> listByClient(String clientCode) {
        List<AgentClientRel> list = relMapper.selectList(new LambdaQueryWrapper<AgentClientRel>()
                .eq(AgentClientRel::getClientCode, clientCode)
                .orderByDesc(AgentClientRel::getBindTime));
        return list.stream().map(this::toVO).toList();
    }

    private AgentClientRelVO toVO(AgentClientRel entity) {
        AgentClientRelVO vo = new AgentClientRelVO();
        vo.setId(entity.getId());
        vo.setAgentCode(entity.getAgentCode());
        vo.setClientCode(entity.getClientCode());
        vo.setBindType(entity.getBindType());
        vo.setBindTime(entity.getBindTime());
        vo.setStatus(entity.getStatus());
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }
}
