package com.dayan.tool.service;

/**
 * AI 生成进度回调（SSE 流式端点消费；非流式调用传 null/no-op）。
 */
public interface AiGenerateProgressListener {

    /** 阶段进展（material/retrieving/composing） */
    void onStage(String stage, String message);

    /** 生成文本增量（仅 composing 阶段） */
    default void onDelta(String text) {
    }

    /** 重置流式预览（自检未过触发自动重写时，前端清空已推送文本） */
    default void onReset() {
    }
}
