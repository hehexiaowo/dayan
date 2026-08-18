package com.dayan.tool.service;

import com.dayan.common.core.exception.BusinessException;
import com.dayan.common.mybatis.context.ContextHolder;
import com.dayan.tool.vo.ToolAiCreatorVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

/**
 * AI 创作流式端点编排：SSE emitter 生命周期 + 虚拟线程上下文恢复 + done/error 收尾。
 * 正文与配图两路共用 {@link #runAsync}；stage/delta/image 事件由各 listener 发出。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ToolAiCreatorStreamService {

    /** SSE 超时（正文三步 + 配图轮询最长耗时） */
    private static final long SSE_TIMEOUT_MS = 300_000L;

    private final ToolAiCreatorPipelineService pipelineService;

    /** 正文流式（body→audit→polish）：事件 stage/delta/done/error */
    public SseEmitter bodyStream(Long id) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
        AtomicBoolean cancelled = cancelledFlag(emitter);
        AiGenerateProgressListener listener = new AiGenerateProgressListener() {
            @Override
            public void onStage(String stage, String message) {
                if (cancelled.get()) {
                    return;
                }
                send(emitter, "stage", Map.of("stage", stage, "message", message));
            }

            @Override
            public void onDelta(String text) {
                if (cancelled.get()) {
                    return;
                }
                send(emitter, "delta", Map.of("text", text));
            }
        };
        runAsync(emitter, "ai-body-stream-" + id, "生成失败，请稍后重试",
                () -> pipelineService.bodyStream(id, listener));
        return emitter;
    }

    /** 配图流式（逐张进度）：事件 stage/image/done/error */
    public SseEmitter imagesStream(Long id) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
        AtomicBoolean cancelled = cancelledFlag(emitter);
        AiImageProgressListener listener = new AiImageProgressListener() {
            @Override
            public void onStage(String stage, String message) {
                if (cancelled.get()) {
                    return;
                }
                send(emitter, "stage", Map.of("stage", stage, "message", message));
            }

            @Override
            public void onImage(String placeholder, String state, String url, String error) {
                if (cancelled.get()) {
                    return;
                }
                Map<String, Object> data = new HashMap<>();
                data.put("placeholder", placeholder);
                data.put("state", state);
                data.put("url", url);
                data.put("error", error);
                send(emitter, "image", data);
            }
        };
        runAsync(emitter, "ai-images-stream-" + id, "配图失败，请稍后重试",
                () -> pipelineService.imagesStream(id, listener));
        return emitter;
    }

    // ---------- 内部设施 ----------

    private AtomicBoolean cancelledFlag(SseEmitter emitter) {
        AtomicBoolean cancelled = new AtomicBoolean(false);
        emitter.onCompletion(() -> cancelled.set(true));
        emitter.onTimeout(() -> {
            cancelled.set(true);
            emitter.complete();
        });
        return cancelled;
    }

    private void send(SseEmitter emitter, String event, Object data) {
        try {
            emitter.send(SseEmitter.event().name(event).data(data, MediaType.APPLICATION_JSON));
        } catch (Exception e) {
            log.debug("SSE {} 发送失败: {}", event, e.getMessage());
        }
    }

    /**
     * 虚拟线程不继承请求线程 ThreadLocal：先捕获登录上下文再在异步线程内恢复
     * （channelCode 空→渠道校验失败；accountType 空→租户拦截器误判放行）。
     */
    private void runAsync(SseEmitter emitter, String threadName, String failMessage,
                          Supplier<ToolAiCreatorVO> task) {
        final String ctxChannelCode = ContextHolder.getChannelCode();
        final String ctxAccountCode = ContextHolder.getAccountCode();
        final String ctxAccountType = ContextHolder.getAccountType();
        final String ctxAccountName = ContextHolder.getAccountName();
        Thread.ofVirtual().name(threadName).start(() -> {
            ContextHolder.setChannelCode(ctxChannelCode);
            ContextHolder.setAccountCode(ctxAccountCode);
            ContextHolder.setAccountType(ctxAccountType);
            ContextHolder.setAccountName(ctxAccountName);
            try {
                ToolAiCreatorVO result = task.get();
                emitter.send(SseEmitter.event().name("done").data(result, MediaType.APPLICATION_JSON));
                emitter.complete();
            } catch (Exception e) {
                String msg = e instanceof BusinessException be ? be.getMessage() : failMessage;
                log.warn("AI 流式失败 {}: {}", threadName, e.getMessage());
                try {
                    emitter.send(SseEmitter.event().name("error")
                            .data(Map.of("message", msg), MediaType.APPLICATION_JSON));
                } catch (Exception ignored) {
                    // 客户端已断开
                }
                emitter.complete();
            }
        });
    }
}
