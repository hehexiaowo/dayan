package com.dayan.knowledge.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/** 知识检索（agent 端 AI 创作素材取材用；渠道可见性同 /repos） */
@Data
public class KnowledgeRetrieveDTO {
    /** 仓库 id（须为当前渠道可见库） */
    @NotNull(message = "仓库必选")
    private Long repoId;
    /** 检索词（主题或文档名） */
    @NotBlank(message = "检索词必填")
    private String query;
    /** 限定文档（可选，勾选文档精准召回） */
    private List<String> docFileIds;
    /** 召回条数，默认 6 */
    private Integer topK;
}
