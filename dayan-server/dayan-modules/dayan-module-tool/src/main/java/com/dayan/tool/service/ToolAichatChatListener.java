package com.dayan.tool.service;

/**
 * AI 问答流式监听器。
 *
 * <p>由 SSE 编排层（{@code ToolAichatStreamService}）实现，问答服务在流式生成过程中
 * 通过回调逐段推送增量文本；{@code onStage} 用于阶段事件（如检索中/生成中）。
 */
public interface ToolAichatChatListener {

    /** 阶段事件回调（如检索中/生成中） */
    void onStage(String stage, String message);

    /** 增量文本回调（每个非空 delta 触发一次） */
    void onDelta(String text);
}