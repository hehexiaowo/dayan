package com.dayan.agent.service.impl;

import com.dayan.agent.dto.AgentContentCreateDTO;
import com.dayan.agent.service.AgentContentService;
import com.dayan.tool.dto.ToolAiCreatorContentCmd;
import com.dayan.tool.service.ToolAiCreatorContentSaver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * tool 域 AI 创作成品落内容中心的 port 实现（agent_content 属 agent 域）。
 */
@Service
@RequiredArgsConstructor
public class ToolAiCreatorContentSaverImpl implements ToolAiCreatorContentSaver {

    private final AgentContentService agentContentService;

    @Override
    public Long save(ToolAiCreatorContentCmd cmd) {
        AgentContentCreateDTO dto = new AgentContentCreateDTO();
        dto.setTitle(cmd.getTitle());
        dto.setContentType(cmd.getContentType());
        dto.setContentBody(cmd.getContentBody());
        dto.setStyleCode(cmd.getStyleCode());
        dto.setAudience(cmd.getAudience());
        dto.setPurpose(cmd.getPurpose());
        dto.setRefContentCode(cmd.getRefContentCode());
        dto.setRefKbFiles(cmd.getRefKbFiles());
        dto.setRefGoodsCodes(cmd.getRefGoodsCodes());
        dto.setCoverImage(cmd.getCoverImage());
        return agentContentService.create(dto);
    }
}
