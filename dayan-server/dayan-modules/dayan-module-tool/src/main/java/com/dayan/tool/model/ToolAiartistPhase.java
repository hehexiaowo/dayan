package com.dayan.tool.model;

/** AI 创作项目阶段状态（状态机见 ToolAiCreatorPipelineServiceImpl 注释） */
public final class ToolAiCreatorPhase {
    public static final String CREATED = "CREATED";
    public static final String DIGESTED = "DIGESTED";
    public static final String STRATEGY_CONFIRMED = "STRATEGY_CONFIRMED";
    public static final String OUTLINE_CONFIRMED = "OUTLINE_CONFIRMED";
    public static final String BODY_DONE = "BODY_DONE";
    public static final String IMAGES_DONE = "IMAGES_DONE";
    public static final String SAVED = "SAVED";
    private ToolAiCreatorPhase() {}
}
