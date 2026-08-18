package com.dayan.tool.service;

import com.dayan.common.core.exception.BusinessException;
import com.dayan.common.mybatis.context.ContextHolder;
import com.dayan.tool.dto.ToolAichatChatDTO;
import com.dayan.tool.vo.ToolAichatChatResultVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@Slf4j
@RequiredArgsConstructor
public class ToolAichatStreamService {

    private static final long SSE_TIMEOUT_MS = 120_000L;
    private final ToolAichatChatService chatService;

    /** 问答流式：事件 stage/delta/done/error（复用 ToolAiartistStreamService 模式） */
    public SseEmitter chatStream(ToolAichatChatDTO dto) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
        AtomicBoolean cancelled = new AtomicBoolean(false);
        emitter.onCompletion(() -> cancelled.set(true));
        emitter.onTimeout(() -> { cancelled.set(true); emitter.complete(); });

        final String ctxChannel = ContextHolder.getChannelCode();
        final String ctxAccount = ContextHolder.getAccountCode();
        final String ctxType = ContextHolder.getAccountType();
        final String ctxName = ContextHolder.getAccountName();
        Thread.ofVirtual().name("ai-qa-chat").start(() -> {
            ContextHolder.setChannelCode(ctxChannel);
            ContextHolder.setAccountCode(ctxAccount);
            ContextHolder.setAccountType(ctxType);
            ContextHolder.setAccountName(ctxName);
            try {
                ToolAichatChatResultVO result = chatService.chatStreamBlocking(dto, new ToolAichatChatListener() {
                    @Override public void onStage(String s, String m) { send(emitter, "stage", Map.of("stage", s, "message", m)); }
                    @Override public void onDelta(String t) { if (!cancelled.get()) send(emitter, "delta", Map.of("text", t)); }
                });
                emitter.send(SseEmitter.event().name("done").data(result, MediaType.APPLICATION_JSON));
                emitter.complete();
            } catch (Exception e) {
                String msg = e instanceof BusinessException be ? be.getMessage() : "问答失败，请稍后重试";
                log.warn("AI 问答流式失败: {}", e.getMessage());
                try { emitter.send(SseEmitter.event().name("error").data(Map.of("message", msg), MediaType.APPLICATION_JSON)); } catch (Exception ignored) {}
                emitter.complete();
            }
        });
        return emitter;
    }

    private void send(SseEmitter emitter, String event, Object data) {
        try { emitter.send(SseEmitter.event().name(event).data(data, MediaType.APPLICATION_JSON)); }
        catch (Exception e) { log.debug("SSE {} 发送失败: {}", event, e.getMessage()); }
    }
}
