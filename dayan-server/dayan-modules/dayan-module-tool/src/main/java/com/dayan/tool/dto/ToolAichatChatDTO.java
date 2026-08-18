package com.dayan.tool.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ToolAichatChatDTO {
    /** 问答人物（tool_info.tool_code，tool_type=aichat） */
    @NotBlank(message = "人物不能为空")
    private String toolCode;
    /** 会话编码（新建时为 null） */
    private String sessionCode;
    @NotBlank(message = "问题不能为空")
    private String question;
}
