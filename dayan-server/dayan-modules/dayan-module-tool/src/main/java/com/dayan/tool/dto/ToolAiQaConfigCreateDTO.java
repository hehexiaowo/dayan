package com.dayan.tool.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class ToolAiQaConfigCreateDTO {
    @NotBlank(message = "人物名称不能为空")
    private String personaName;
    private String icon;
    private String iconColor;
    @NotBlank(message = "人设描述不能为空")
    private String systemPrompt;
    private String welcomeMsg;
    private List<String> recommendQuestions;
    private List<Long> repoIds;
    private Integer sortOrder;
    private Integer status;
    private String remark;
}
