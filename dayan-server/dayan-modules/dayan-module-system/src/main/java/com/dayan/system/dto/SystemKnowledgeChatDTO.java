package com.dayan.system.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 知识库问答 DTO（RAG：检索命中 + 大模型生成）。
 */
@Data
public class SystemKnowledgeChatDTO {

    /** 用户问题 */
    @NotBlank(message = "问题不能为空")
    private String question;

    /** 召回片段数（默认 4） */
    private Integer topK;
}
