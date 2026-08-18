package com.dayan.tool.service;

import com.dayan.tool.dto.ToolAiCreatorContentCmd;

/**
 * 保存成品到内容中心的 port：内容中心属 agent 域（agent_content），
 * tool 域经此接口回调，由 agent 模块提供实现，避免 tool→agent 反向依赖。
 */
public interface ToolAiCreatorContentSaver {

    /**
     * 保存成品，返回 agent_content 主键。
     *
     * @param cmd 成品内容（字段与 AgentContentCreateDTO 一一对应）
     * @return 内容主键
     */
    Long save(ToolAiCreatorContentCmd cmd);
}
