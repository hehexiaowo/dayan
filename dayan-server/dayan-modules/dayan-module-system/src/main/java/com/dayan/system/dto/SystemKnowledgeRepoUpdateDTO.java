package com.dayan.system.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 知识仓库更新 DTO（仅允许改名称/描述/排序；归属与远端索引不可改）。
 */
@Data
public class SystemKnowledgeRepoUpdateDTO {

    @Size(max = 100, message = "仓库名称最长 100 字")
    private String repoName;

    @Size(max = 255, message = "描述最长 255 字")
    private String description;

    /** 索引配置（整体替换；已建库仅检索参数可改，切分等不可变字段变更会被拒绝） */
    private SystemKnowledgeIndexConfig indexConfig;

    private Integer sortOrder;
}
