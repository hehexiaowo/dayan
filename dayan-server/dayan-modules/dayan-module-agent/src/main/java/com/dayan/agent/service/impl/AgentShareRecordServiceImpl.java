package com.dayan.agent.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dayan.agent.dto.AgentShareRecordCreateDTO;
import com.dayan.agent.dto.AgentShareRecordQueryDTO;
import com.dayan.agent.entity.AgentShareRecord;
import com.dayan.agent.mapper.AgentShareRecordMapper;
import com.dayan.agent.service.AgentShareRecordService;
import com.dayan.agent.vo.AgentShareRecordVO;
import com.dayan.common.core.resp.PageResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 代理人分享记录（agent_share_record）服务实现。
 *
 * <p>share_code 用 UUID 去横线生成（uk_share_code 兜底唯一）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgentShareRecordServiceImpl implements AgentShareRecordService {

    private final AgentShareRecordMapper shareRecordMapper;

    @Override
    public PageResult<AgentShareRecordVO> page(AgentShareRecordQueryDTO query) {
        LambdaQueryWrapper<AgentShareRecord> wrapper = new LambdaQueryWrapper<AgentShareRecord>()
                .orderByDesc(AgentShareRecord::getShareTime);
        if (query.getAgentCode() != null && !query.getAgentCode().isEmpty()) {
            wrapper.eq(AgentShareRecord::getAgentCode, query.getAgentCode());
        }
        if (query.getAgentCodes() != null && !query.getAgentCodes().isEmpty()) {
            wrapper.in(AgentShareRecord::getAgentCode, query.getAgentCodes());
        }
        if (query.getShareCode() != null && !query.getShareCode().isEmpty()) {
            wrapper.eq(AgentShareRecord::getShareCode, query.getShareCode());
        }
        if (query.getShareType() != null) {
            wrapper.eq(AgentShareRecord::getShareType, query.getShareType());
        }
        if (query.getClientCode() != null && !query.getClientCode().isEmpty()) {
            wrapper.eq(AgentShareRecord::getClientCode, query.getClientCode());
        }
        Page<AgentShareRecord> page = shareRecordMapper.selectPage(
                new Page<>(query.getCurrent(), query.getSize()), wrapper);
        List<AgentShareRecordVO> records = page.getRecords().stream().map(this::toVO).toList();
        return new PageResult<>(query.getCurrent(), query.getSize(), page.getTotal(), records);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String create(AgentShareRecordCreateDTO dto) {
        AgentShareRecord entity = new AgentShareRecord();
        entity.setShareCode(generateShareCode());
        entity.setAgentCode(dto.getAgentCode());
        entity.setShareType(dto.getShareType());
        entity.setBizCode(dto.getBizCode());
        entity.setShareChannel(dto.getShareChannel());
        entity.setClientCode(dto.getClientCode());
        entity.setViewCount(0);
        entity.setShareTime(LocalDateTime.now());
        shareRecordMapper.insert(entity);
        log.info("创建分享记录: shareCode={}, agentCode={}, type={}",
                entity.getShareCode(), dto.getAgentCode(), dto.getShareType());
        return entity.getShareCode();
    }

    @Override
    public List<AgentShareRecordVO> listByAgent(String agentCode) {
        List<AgentShareRecord> list = shareRecordMapper.selectList(new LambdaQueryWrapper<AgentShareRecord>()
                .eq(AgentShareRecord::getAgentCode, agentCode)
                .orderByDesc(AgentShareRecord::getShareTime));
        return list.stream().map(this::toVO).toList();
    }

    /** share_code：UUID 去横线 */
    private String generateShareCode() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private AgentShareRecordVO toVO(AgentShareRecord entity) {
        AgentShareRecordVO vo = new AgentShareRecordVO();
        vo.setId(entity.getId());
        vo.setShareCode(entity.getShareCode());
        vo.setAgentCode(entity.getAgentCode());
        vo.setShareType(entity.getShareType());
        vo.setBizCode(entity.getBizCode());
        vo.setShareChannel(entity.getShareChannel());
        vo.setClientCode(entity.getClientCode());
        vo.setViewCount(entity.getViewCount());
        vo.setShareTime(entity.getShareTime());
        vo.setCreatedAt(entity.getCreatedAt());
        return vo;
    }
}
