package com.dayan.tool.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dayan.common.core.code.CodeGenerator;
import com.dayan.common.core.exception.BusinessException;
import com.dayan.common.core.exception.ErrorCode;
import com.dayan.tool.entity.ToolAichatMessage;
import com.dayan.tool.entity.ToolAichatSession;
import com.dayan.tool.mapper.ToolAichatMessageMapper;
import com.dayan.tool.mapper.ToolAichatSessionMapper;
import com.dayan.tool.service.ToolAichatSessionService;
import com.dayan.tool.service.ToolInfoService;
import com.dayan.tool.vo.ToolAichatPersonaVO;
import com.dayan.tool.vo.ToolAichatSessionVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ToolAichatSessionServiceImpl implements ToolAichatSessionService {

    private final ToolAichatSessionMapper sessionMapper;
    private final ToolAichatMessageMapper messageMapper;
    private final ToolInfoService toolInfoService;
    private final CodeGenerator codeGenerator;

    @Override
    public List<ToolAichatSessionVO> listByTool(String agentCode, String toolCode) {
        return sessionMapper.selectList(new LambdaQueryWrapper<ToolAichatSession>()
                .eq(ToolAichatSession::getAgentCode, agentCode)
                .eq(ToolAichatSession::getToolCode, toolCode)
                .orderByDesc(ToolAichatSession::getLastMessageAt)
                .orderByDesc(ToolAichatSession::getId)).stream()
                .map(this::toVO).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String create(String agentCode, String channelCode, String toolCode) {
        ToolAichatPersonaVO persona = toolInfoService.getQaPersona(toolCode);
        ToolAichatSession session = new ToolAichatSession();
        session.setSessionCode(codeGenerator.generate("QAS"));
        session.setToolCode(persona.getToolCode());
        session.setPersonaName(persona.getPersonaName());
        session.setAgentCode(agentCode);
        session.setChannelCode(channelCode);
        session.setTitle("与" + persona.getPersonaName() + "的对话");
        session.setMessageCount(0);
        sessionMapper.insert(session);
        return session.getSessionCode();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(String agentCode, String sessionCode) {
        ToolAichatSession session = requireOwned(agentCode, sessionCode);
        sessionMapper.deleteById(session.getId());
        // 物理删除该会话全部消息
        messageMapper.delete(new LambdaQueryWrapper<ToolAichatMessage>()
                .eq(ToolAichatMessage::getSessionCode, sessionCode));
    }

    private ToolAichatSession requireOwned(String agentCode, String sessionCode) {
        ToolAichatSession session = sessionMapper.selectOne(new LambdaQueryWrapper<ToolAichatSession>()
                .eq(ToolAichatSession::getSessionCode, sessionCode)
                .eq(ToolAichatSession::getAgentCode, agentCode)
                .last("LIMIT 1"));
        if (session == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "会话不存在");
        }
        return session;
    }

    private ToolAichatSessionVO toVO(ToolAichatSession s) {
        ToolAichatSessionVO vo = new ToolAichatSessionVO();
        vo.setId(s.getId());
        vo.setSessionCode(s.getSessionCode());
        vo.setToolCode(s.getToolCode());
        vo.setPersonaName(s.getPersonaName());
        vo.setTitle(s.getTitle());
        vo.setMessageCount(s.getMessageCount());
        vo.setLastMessageAt(s.getLastMessageAt());
        vo.setCreatedAt(s.getCreatedAt());
        return vo;
    }
}
