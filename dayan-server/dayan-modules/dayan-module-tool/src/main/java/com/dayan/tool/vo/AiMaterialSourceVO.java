package com.dayan.tool.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AI 生成素材来源（知识库引用片段回显，供代理人核对事实出处）。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiMaterialSourceVO {

    /** 来源仓库名 */
    private String repoName;

    /** 片段文本（截断） */
    private String text;
}
