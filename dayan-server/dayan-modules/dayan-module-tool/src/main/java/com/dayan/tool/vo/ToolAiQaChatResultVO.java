package com.dayan.tool.vo;

import lombok.Data;

import java.util.List;

@Data
public class ToolAiQaChatResultVO {
    private String answer;
    private List<Citation> citations;
    private String sessionCode;

    @Data
    public static class Citation {
        private String text;
        private Double score;
        private Long repoId;
        private String repoName;
        private String docId;
        private String docName;
    }
}
