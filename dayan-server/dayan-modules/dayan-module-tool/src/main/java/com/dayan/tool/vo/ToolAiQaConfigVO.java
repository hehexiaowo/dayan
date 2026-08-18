package com.dayan.tool.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ToolAiQaConfigVO {
    private Long id;
    private String configCode;
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
    private LocalDateTime createdAt;
}
