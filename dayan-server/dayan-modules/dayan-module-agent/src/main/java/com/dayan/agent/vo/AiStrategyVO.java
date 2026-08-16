package com.dayan.agent.vo;

import lombok.Data;

/** 策略面板（strategy JSON 结构，含隐形作战指令） */
@Data
public class AiStrategyVO {
    private String targetAudience;
    private String corePainPoint;
    private String viralLogic;
    private String advantageHook;
    private String coreExecutionPrompt;
}
