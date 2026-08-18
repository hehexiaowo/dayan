package com.dayan.tool.vo;

import lombok.Data;

/**
 * AI 创作分类 VO（由 tool_info 的 aiartist 实例组装，分类配置来自 config_json）。
 */
@Data
public class ToolAiartistConfigVO {

    /** 工具实例编码（TL 前缀） */
    private String toolCode;

    /** 分类名称（= tool_info.tool_name） */
    private String toolName;

    /** 分类简介 */
    private String toolDesc;

    /** 创作目的（product/park/science，config_json 预置） */
    private String purpose;

    /** 图标（文字或图标名） */
    private String icon;

    /** 图标颜色（blue/green/orange/red/gray） */
    private String iconColor;

    /** 分类人设（config_json.systemPrompt，注入流水线提示词） */
    private String systemPrompt;
}
