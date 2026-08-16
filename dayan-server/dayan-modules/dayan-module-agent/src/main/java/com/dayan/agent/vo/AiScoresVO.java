package com.dayan.agent.vo;

import lombok.Data;

/** 五维打分 + 主编点评 */
@Data
public class AiScoresVO {
    private Double naturalness;
    private Double viralDesign;
    private Double styleSimilarity;
    private Double emotionalImpact;
    private Double conversionRate;
    private String editorCritique;
}
