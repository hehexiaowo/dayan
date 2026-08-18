package com.dayan.tool.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dayan.common.core.code.CodeGenerator;
import com.dayan.common.core.exception.BusinessException;
import com.dayan.common.core.exception.ErrorCode;
import com.dayan.tool.entity.ToolAiQaConfig;
import com.dayan.tool.entity.ToolAiQaMessage;
import com.dayan.tool.entity.ToolAiQaSession;
import com.dayan.tool.mapper.ToolAiQaConfigMapper;
import com.dayan.tool.mapper.ToolAiQaMessageMapper;
import com.dayan.tool.mapper.ToolAiQaSessionMapper;
import com.dayan.tool.service.ToolAiQaSessionService;
import com.dayan.tool.vo.ToolAiQaSessionVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ToolAiQaSessionServiceImpl implements ToolAiQaSessionService {

    private final ToolAiQaSessionMapper sessionMapper;
    private final ToolAiQaConfigMapper configMapper;
    private final ToolAiQaMessageMapper messageMapper;
    private final CodeGenerator codeGenerator;

    @Override
    public List<ToolAiQaSessionVO> listByPersona(String agentCode, Long configId) {
        return sessionMapper.selectList(new LambdaQueryWrapper<ToolAiQaSession>()
                .eq(ToolAiQaSession::getAgentCode, agentCode)
                .eq(ToolAiQaSession::getConfigId, configId)
                .orderByDesc(ToolAiQaSession::getLastMessageAt)
                .orderByDesc(ToolAiQaSession::getId)).stream()
                .map(this::toVO).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String create(String agentCode, String channelCode, Long configId, String toolCode) {
        ToolAiQaConfig config = configMapper.selectById(configId);
        if (config == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "人物配置不存在: " + configId);
        }
        ToolAiQaSession session = new ToolAiQaSession();
        session.setSessionCode(codeGenerator.generate("QAS"));
        session.setToolCode(toolCode == null || toolCode.isBlank() ? "TL00004" : toolCode);
        session.setConfigId(config.getId());
        session.setConfigCode(config.getConfigCode());
        session.setPersonaName(config.getPersonaName());
        session.setAgentCode(agentCode);
        session.setChannelCode(channelCode);
        session.setTitle("与" + config.getPersonaName() + "的对话");
        session.setMessageCount(0);
        sessionMapper.insert(session);
        return session.getSessionCode();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(String agentCode, String sessionCode) {
        ToolAiQaSession session = requireOwned(agentCode, sessionCode);
        sessionMapper.deleteById(session.getId());
        // 物理删除该会话全部消息
        messageMapper.delete(new LambdaQueryWrapper<ToolAiQaMessage>()
                .eq(ToolAiQaMessage::getSessionCode, sessionCode));
    }

    private ToolAiQaSession requireOwned(String agentCode, String sessionCode) {
        ToolAiQaSession session = sessionMapper.selectOne(new LambdaQueryWrapper<ToolAiQaSession>()
                .eq(ToolAiQaSession::getSessionCode, sessionCode)
                .eq(ToolAiQaSession::getAgentCode, agentCode)
                .last("LIMIT 1"));
        if (session == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "会话不存在");
        }
        return session;
    }

    private ToolAiQaSessionVO toVO(ToolAiQaSession s) {
        ToolAiQaSessionVO vo = new ToolAiQaSessionVO();
        vo.setId(s.getId());
        vo.setSessionCode(s.getSessionCode());
        vo.setToolCode(s.getToolCode());
        vo.setConfigId(s.getConfigId());
        vo.setConfigCode(s.getConfigCode());
        vo.setPersonaName(s.getPersonaName());
        vo.setTitle(s.getTitle());
        vo.setMessageCount(s.getMessageCount());
        vo.setLastMessageAt(s.getLastMessageAt());
        vo.setCreatedAt(s.getCreatedAt());
        return vo;
    }
}
