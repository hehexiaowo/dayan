package com.dayan.knowledge.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * 文档导入索引 DTO（解析成功的文档追加进知识库索引）。
 */
@Data
public class KnowledgeDocImportDTO {

    /** 百炼文件 ID 列表（须已解析成功 PARSE_SUCCESS） */
    @NotEmpty(message = "文件列表不能为空")
    private List<String> fileIds;
}
