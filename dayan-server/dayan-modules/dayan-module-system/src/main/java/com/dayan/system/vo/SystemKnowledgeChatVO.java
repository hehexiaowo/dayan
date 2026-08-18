package com.dayan.knowledge.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 知识库问答 VO（答案 + 引用片段）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeChatVO {

    /** 模型生成的回答 */
    private String answer;

    /** 检索命中的引用片段 */
    private List<Citation> citations;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Citation {
        /** 片段内容 */
        private String text;
        /** 相关度得分 */
        private Double score;
    }
}
