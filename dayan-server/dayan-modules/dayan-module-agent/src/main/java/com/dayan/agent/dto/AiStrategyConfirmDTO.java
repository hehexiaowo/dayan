package com.dayan.agent.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 确认策略：选定标题 + 可选策略面板字段微调 */
@Data
public class AiStrategyConfirmDTO {
    @NotBlank(message = "请选择一个标题")
    @Size(max = 200, message = "标题长度不能超过 200")
    private String selectedTitle;
    @Size(max = 500, message = "受众画像不能超过 500 字")
    private String targetAudience;
    @Size(max = 500, message = "核心痛点不能超过 500 字")
    private String corePainPoint;
    @Size(max = 500, message = "爆款逻辑不能超过 500 字")
    private String viralLogic;
    @Size(max = 800, message = "优势放大器不能超过 800 字")
    private String advantageHook;
}
