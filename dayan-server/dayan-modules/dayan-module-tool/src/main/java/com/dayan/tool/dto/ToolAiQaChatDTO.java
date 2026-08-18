package com.dayan.tool.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ToolAiQaChatDTO {
    @NotNull(message = "人物不能为空")
    private Long configId;
    /** 会话编码（新建时为 null） */
    private String sessionCode;
    @NotBlank(message = "问题不能为空")
    private String question;
}
