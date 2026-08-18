package com.dayan.tool.dto;

import lombok.Data;

import java.util.List;

/** AI 问答人物更新（各字段可空做部分更新，不做必填校验） */
@Data
public class ToolAiQaConfigUpdateDTO {
    private String personaName;
    private String icon;
    private String iconColor;
    private String systemPrompt;
    private String welcomeMsg;
    private List<String> recommendQuestions;
    private List<Long> repoIds;
    private Integer sortOrder;
    private Integer status;
    private String remark;
}
