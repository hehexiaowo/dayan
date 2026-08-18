package com.dayan.tool.vo;

import lombok.Data;

import java.util.List;

/**
 * AI 问答人物 VO（由 tool_info 的 aichat 实例组装，人物属性来自 config_json）。
 */
@Data
public class ToolAichatPersonaVO {

    /** 工具实例编码（TL 前缀） */
    private String toolCode;

    /** 人物名称（= tool_info.tool_name） */
    private String personaName;

    /** 头像（文字或图标名） */
    private String icon;

    /** 头像颜色（blue/green/orange/red/gray） */
    private String iconColor;

    /** 人设描述（注入 system prompt） */
    private String systemPrompt;

    /** 开场白 */
    private String welcomeMsg;

    /** 推荐问题 */
    private List<String> recommendQuestions;

    /** 绑定知识库 ID */
    private List<Long> repoIds;
}
