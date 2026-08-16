package com.dayan.agent.vo;

import lombok.Data;

import java.util.List;

/** 策略阶段 LLM 输出包裹（strategy_panel + generated_titles + core_execution_prompt） */
@Data
public class AiStrategyBundleVO {
    private AiStrategyVO strategyPanel;
    private List<AiTitleVO> generatedTitles;
    private String coreExecutionPrompt;
}
