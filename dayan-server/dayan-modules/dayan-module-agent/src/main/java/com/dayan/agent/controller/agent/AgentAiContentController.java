package com.dayan.agent.controller.agent;

import com.dayan.agent.dto.AgentContentCreateDTO;
import com.dayan.agent.dto.AgentContentQueryDTO;
import com.dayan.agent.dto.AgentContentUpdateDTO;
import com.dayan.agent.dto.AiGenerateDTO;
import com.dayan.agent.service.AgentContentService;
import com.dayan.agent.service.AiContentGenerateService;
import com.dayan.agent.service.AiGenerateProgressListener;
import com.dayan.agent.vo.AgentContentVO;
import com.dayan.agent.vo.AiGenerateResultVO;
import com.dayan.common.core.exception.BusinessException;
import com.dayan.common.mybatis.context.ContextHolder;
import com.dayan.common.core.resp.PageResult;
import com.dayan.common.core.resp.R;
import com.dayan.agent.model.AiRefTemplates;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

/**
 * Agent AI 内容接口：生成（不落库）+ 个人内容 CRUD。
 *
 * <p>路径 {@code /agent-api/ai/...}。agentCode 服务端从登录上下文注入，防越权。
 */
@Slf4j
@Tag(name = "Agent AI 内容")
@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
public class AgentAiContentController {

    private final AgentContentService agentContentService;
    private final AiContentGenerateService aiContentGenerateService;

    @Operation(summary = "AI 生成内容（不落库，预览用）")
    @PostMapping("/generate")
    public R<AiGenerateResultVO> generate(@RequestBody @Valid AiGenerateDTO dto) {
        return R.ok(aiContentGenerateService.generate(dto));
    }

    @Operation(summary = "AI 生成内容（SSE 流式：stage/delta/done/error 事件）")
    @PostMapping("/generate/stream")
    public SseEmitter generateStream(@RequestBody @Valid AiGenerateDTO dto) {
        SseEmitter emitter = new SseEmitter(150_000L);
        AiGenerateProgressListener listener = new AiGenerateProgressListener() {
            @Override
            public void onStage(String stage, String message) {
                try {
                    emitter.send(SseEmitter.event().name("stage")
                            .data(java.util.Map.of("stage", stage, "message", message), MediaType.APPLICATION_JSON));
                } catch (Exception e) {
                    log.warn("SSE stage 发送失败: {}", e.getMessage());
                }
            }

            @Override
            public void onDelta(String text) {
                try {
                    emitter.send(SseEmitter.event().name("delta")
                            .data(java.util.Map.of("text", text), MediaType.APPLICATION_JSON));
                } catch (Exception e) {
                    log.warn("SSE delta 发送失败: {}", e.getMessage());
                }
            }
        };
        // 虚拟线程不继承请求线程的 ThreadLocal（SaTokenContextFilter 仅在请求线程设置），
        // 先捕获登录上下文再在异步线程内恢复——否则 channelCode 为空导致渠道校验失败，
        // accountType 为空会让租户拦截器误判放行（安全隐患）
        final String ctxChannelCode = ContextHolder.getChannelCode();
        final String ctxAccountCode = ContextHolder.getAccountCode();
        final String ctxAccountType = ContextHolder.getAccountType();
        final String ctxAccountName = ContextHolder.getAccountName();
        Thread.ofVirtual().name("ai-generate-stream").start(() -> {
            ContextHolder.setChannelCode(ctxChannelCode);
            ContextHolder.setAccountCode(ctxAccountCode);
            ContextHolder.setAccountType(ctxAccountType);
            ContextHolder.setAccountName(ctxAccountName);
            try {
                AiGenerateResultVO result = aiContentGenerateService.generate(dto, listener);
                emitter.send(SseEmitter.event().name("done").data(result, MediaType.APPLICATION_JSON));
                emitter.complete();
            } catch (Exception e) {
                String msg = e instanceof BusinessException be
                        ? be.getMessage() : "生成失败，请稍后重试";
                log.warn("AI 流式生成失败: {}", e.getMessage());
                try {
                    emitter.send(SseEmitter.event().name("error")
                            .data(java.util.Map.of("message", msg), MediaType.APPLICATION_JSON));
                } catch (Exception ignore) {
                    // 客户端已断开
                }
                emitter.complete();
            }
        });
        return emitter;
    }

    @Operation(summary = "内置范文模板（平台风格参考）")
    @GetMapping("/templates")
    public R<List<AiRefTemplates.RefTemplate>> templates() {
        return R.ok(AiRefTemplates.TEMPLATES);
    }

    @Operation(summary = "保存 AI 生成内容")
    @PostMapping("/contents")
    public R<Long> create(@RequestBody @Valid AgentContentCreateDTO dto) {
        return R.ok(agentContentService.create(dto));
    }

    @Operation(summary = "我的内容分页")
    @GetMapping("/contents")
    public R<PageResult<AgentContentVO>> page(AgentContentQueryDTO query) {
        return R.ok(agentContentService.page(query));
    }

    @Operation(summary = "我的内容详情")
    @GetMapping("/contents/{id}")
    public R<AgentContentVO> detail(@PathVariable Long id) {
        return R.ok(agentContentService.getDetail(id));
    }

    @Operation(summary = "编辑我的内容")
    @PutMapping("/contents/{id}")
    public R<Void> update(@PathVariable Long id, @RequestBody @Valid AgentContentUpdateDTO dto) {
        agentContentService.update(id, dto);
        return R.ok();
    }

    @Operation(summary = "删除我的内容")
    @DeleteMapping("/contents/{id}")
    public R<Void> delete(@PathVariable Long id) {
        agentContentService.delete(id);
        return R.ok();
    }
}
