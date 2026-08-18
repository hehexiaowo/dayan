package com.dayan.tool.vo;

import lombok.Data;

import java.util.List;

/** 素材消化产物（fact_digest JSON 结构） */
@Data
public class AiFactDigestVO {
    private List<HardFact> hardFacts;
    private List<String> softPoints;
    private List<String> missing;
    @Data
    public static class HardFact {
        private String fact;
        private String source;
    }
}
