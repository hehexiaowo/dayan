package com.dayan.agent.service;

/** 配图生成进度回调（SSE 转发用） */
public interface AiImageProgressListener {
    void onStage(String stage, String message);
    /** state: generating/done/failed/skipped */
    void onImage(String placeholder, String state, String url, String error);
}
