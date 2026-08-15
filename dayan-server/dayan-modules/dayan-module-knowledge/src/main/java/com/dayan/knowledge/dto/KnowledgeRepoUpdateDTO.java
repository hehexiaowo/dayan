package com.dayan.knowledge.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 知识仓库更新 DTO（仅允许改名称/描述/排序；归属与远端索引不可改）。
 */
@Data
public class KnowledgeRepoUpdateDTO {

    @Size(max = 100, message = "仓库名称最长 100 字")
    private String repoName;

    @Size(max = 255, message = "描述最长 255 字")
    private String description;

    private Integer sortOrder;
}
