package com.dayan.tool.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/** 相关性标签选择（用于价值判断） */
@Data
public class ArticleRewriteValueJudgeDTO {

    /** 用户选定的相关性标签 */
    @NotEmpty(message = "请至少选择一个相关性标签")
    private List<String> selectedTags;
}
