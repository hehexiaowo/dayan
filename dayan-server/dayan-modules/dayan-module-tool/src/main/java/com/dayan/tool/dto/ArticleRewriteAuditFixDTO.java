package com.dayan.tool.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/** 审核修复请求 */
@Data
public class ArticleRewriteAuditFixDTO {

    /** 要修复的审核项索引列表 */
    @NotEmpty(message = "请至少选择一项要修复的内容")
    private List<Integer> itemIndexes;
}
