package com.dayan.agent.controller.agent;

import com.dayan.agent.dto.AiOutlineConfirmDTO;
import com.dayan.agent.dto.AiOutlineRegenDTO;
import com.dayan.agent.dto.AiProjectCreateDTO;
import com.dayan.agent.dto.AiReviseDTO;
import com.dayan.agent.dto.AiStrategyConfirmDTO;
import com.dayan.agent.dto.AiTitleRegenDTO;
import com.dayan.agent.service.AiCreationPipelineService;
import com.dayan.agent.service.AiCreationProjectService;
import com.dayan.agent.service.AiGenerateProgressListener;
import com.dayan.agent.service.AiImageProgressListener;
import com.dayan.common.core.exception.BusinessException;
import com.dayan.common.core.resp.PageResult;
import com.dayan.common.core.resp.R;
import com.dayan.agent.vo.AiProjectListVO;
import com.dayan.agent.vo.AiProjectVO;
import com.dayan.common.mybatis.context.ContextHolder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * AI 创作六阶段流水线。路径 {@code /agent-api/ai/projects/...}。
 * agentCode 服务端从登录上下文注入，防越权。
 */
@Slf4j
@Tag(name = "Agent AI 创作")
@RestController
@RequestMapping("/ai/projects")
@RequiredArgsConstructor
public class AgentAiCreationController {

    private final AiCreationProjectService projectService;
    private final AiCreationPipelineService pipelineService;

    @Operation(summary = "创建创作项目")
    @PostMapping
    public R<Long> create(@RequestBody @Valid AiProjectCreateDTO dto) {
        return R.ok(projectService.create(dto));
    }

    @Operation(summary = "我的创作列表")
    @GetMapping("/list")
    public R<PageResult<AiProjectListVO>> list(long current, long size,
                                               @RequestParam(required = false) String status) {
        return R.ok(projectService.page(current, size, status));
    }

    @Operation(summary = "项目详情（恢复草稿）")
    @GetMapping("/{id}")
    public R<AiProjectVO> detail(@PathVariable Long id) {
        return R.ok(projectService.getDetail(id));
    }

    @Operation(summary = "删除草稿")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        projectService.delete(id);
        return R.ok();
    }

    @Operation(summary = "重跑素材消化")
    @PostMapping("/{id}/digest")
    public R<AiProjectVO> digest(@PathVariable Long id) {
        return R.ok(pipelineService.digest(id));
    }

    @Operation(summary = "生成策略+5标题")
    @PostMapping("/{id}/strategy")
    public R<AiProjectVO> strategy(@PathVariable Long id) {
        return R.ok(pipelineService.strategy(id));
    }

    @Operation(summary = "带反馈重出标题（策略锁定）")
    @PostMapping("/{id}/titles/regenerate")
    public R<AiProjectVO> regenTitles(@PathVariable Long id, @RequestBody(required = false) AiTitleRegenDTO dto) {
        return R.ok(pipelineService.regenerateTitles(id, dto));
    }

    @Operation(summary = "锁定策略+选定标题")
    @PostMapping("/{id}/strategy/confirm")
    public R<AiProjectVO> confirmStrategy(@PathVariable Long id, @RequestBody @Valid AiStrategyConfirmDTO dto) {
        return R.ok(pipelineService.confirmStrategy(id, dto));
    }

    @Operation(summary = "生成大纲")
    @PostMapping("/{id}/outline")
    public R<AiProjectVO> outline(@PathVariable Long id) {
        return R.ok(pipelineService.outline(id));
    }

    @Operation(summary = "带反馈重生成大纲")
    @PostMapping("/{id}/outline/regenerate")
    public R<AiProjectVO> regenOutline(@PathVariable Long id, @RequestBody(required = false) AiOutlineRegenDTO dto) {
        return R.ok(pipelineService.regenerateOutline(id, dto));
    }

    @Operation(summary = "锁定大纲（可含微调）")
    @PostMapping("/{id}/outline/confirm")
    public R<AiProjectVO> confirmOutline(@PathVariable Long id, @RequestBody @Valid AiOutlineConfirmDTO dto) {
        return R.ok(pipelineService.confirmOutline(id, dto));
    }

    @Operation(summary = "生成正文（非流式，小程序降级用）")
    @PostMapping("/{id}/body")
    public R<AiProjectVO> body(@PathVariable Long id) {
        return R.ok(pipelineService.bodyStream(id, null));
    }

    @Operation(summary = "生成正文（SSE：body→audit→polish）")
    @PostMapping("/{id}/body/stream")
    public SseEmitter bodyStream(@PathVariable Long id) {
        SseEmitter emitter = new SseEmitter(300_000L);
        final AtomicBoolean cancelled = new AtomicBoolean(false);
        emitter.onCompletion(() -> cancelled.set(true));
        emitter.onTimeout(() -> {
            cancelled.set(true);
            emitter.complete();
        });
        AiGenerateProgressListener listener = new AiGenerateProgressListener() {
            @Override
            public void onStage(String stage, String message) {
                if (cancelled.get()) {
                    return;
                }
                try {
                    emitter.send(SseEmitter.event().name("stage")
                            .data(java.util.Map.of("stage", stage, "message", message), MediaType.APPLICATION_JSON));
                } catch (Exception e) {
                    log.debug("SSE stage 发送失败: {}", e.getMessage());
                }
            }

            @Override
            public void onDelta(String text) {
                if (cancelled.get()) {
                    return;
                }
                try {
                    emitter.send(SseEmitter.event().name("delta")
                            .data(java.util.Map.of("text", text), MediaType.APPLICATION_JSON));
                } catch (Exception e) {
                    log.debug("SSE delta 发送失败: {}", e.getMessage());
                }
            }
        };
        // 虚拟线程不继承请求线程的 ThreadLocal，先捕获登录上下文再在异步线程内恢复
        //（否则 channelCode 为空导致渠道校验失败、accountType 为空让租户拦截器误判放行）
        final String ctxChannelCode = ContextHolder.getChannelCode();
        final String ctxAccountCode = ContextHolder.getAccountCode();
        final String ctxAccountType = ContextHolder.getAccountType();
        final String ctxAccountName = ContextHolder.getAccountName();
        Thread.ofVirtual().name("ai-body-stream-" + id).start(() -> {
            ContextHolder.setChannelCode(ctxChannelCode);
            ContextHolder.setAccountCode(ctxAccountCode);
            ContextHolder.setAccountType(ctxAccountType);
            ContextHolder.setAccountName(ctxAccountName);
            try {
                AiProjectVO result = pipelineService.bodyStream(id, listener);
                emitter.send(SseEmitter.event().name("done").data(result, MediaType.APPLICATION_JSON));
                emitter.complete();
            } catch (Exception e) {
                String msg = e instanceof BusinessException be ? be.getMessage() : "生成失败，请稍后重试";
                log.warn("AI 正文流式失败 projectId={}: {}", id, e.getMessage());
                try {
                    emitter.send(SseEmitter.event().name("error")
                            .data(java.util.Map.of("message", msg), MediaType.APPLICATION_JSON));
                } catch (Exception ignored) {
                    // 客户端已断开
                }
                emitter.complete();
            }
        });
        return emitter;
    }

    @Operation(summary = "段落勘误（最小化修订）")
    @PostMapping("/{id}/revise")
    public R<AiProjectVO> revise(@PathVariable Long id, @RequestBody @Valid AiReviseDTO dto) {
        return R.ok(pipelineService.revise(id, dto));
    }

    @Operation(summary = "生成配图（SSE：逐张进度）")
    @PostMapping("/{id}/images/stream")
    public SseEmitter imagesStream(@PathVariable Long id) {
        SseEmitter emitter = new SseEmitter(300_000L);
        final AtomicBoolean cancelled = new AtomicBoolean(false);
        emitter.onCompletion(() -> cancelled.set(true));
        emitter.onTimeout(() -> {
            cancelled.set(true);
            emitter.complete();
        });
        AiImageProgressListener listener = new AiImageProgressListener() {
            @Override
            public void onStage(String stage, String message) {
                if (cancelled.get()) {
                    return;
                }
                try {
                    emitter.send(SseEmitter.event().name("stage")
                            .data(java.util.Map.of("stage", stage, "message", message), MediaType.APPLICATION_JSON));
                } catch (Exception e) {
                    log.debug("SSE stage 发送失败: {}", e.getMessage());
                }
            }

            @Override
            public void onImage(String placeholder, String state, String url, String error) {
                if (cancelled.get()) {
                    return;
                }
                java.util.Map<String, Object> data = new java.util.HashMap<>();
                data.put("placeholder", placeholder);
                data.put("state", state);
                data.put("url", url);
                data.put("error", error);
                try {
                    emitter.send(SseEmitter.event().name("image").data(data, MediaType.APPLICATION_JSON));
                } catch (Exception e) {
                    log.debug("SSE image 发送失败: {}", e.getMessage());
                }
            }
        };
        // 虚拟线程恢复登录上下文（同 body/stream 模式，防租户拦截器误判）
        final String ctxChannelCode = ContextHolder.getChannelCode();
        final String ctxAccountCode = ContextHolder.getAccountCode();
        final String ctxAccountType = ContextHolder.getAccountType();
        final String ctxAccountName = ContextHolder.getAccountName();
        Thread.ofVirtual().name("ai-images-stream-" + id).start(() -> {
            ContextHolder.setChannelCode(ctxChannelCode);
            ContextHolder.setAccountCode(ctxAccountCode);
            ContextHolder.setAccountType(ctxAccountType);
            ContextHolder.setAccountName(ctxAccountName);
            try {
                AiProjectVO result = pipelineService.imagesStream(id, listener);
                emitter.send(SseEmitter.event().name("done").data(result, MediaType.APPLICATION_JSON));
                emitter.complete();
            } catch (Exception e) {
                String msg = e instanceof BusinessException be ? be.getMessage() : "配图失败，请稍后重试";
                log.warn("AI 配图失败 projectId={}: {}", id, e.getMessage());
                try {
                    emitter.send(SseEmitter.event().name("error")
                            .data(java.util.Map.of("message", msg), MediaType.APPLICATION_JSON));
                } catch (Exception ignored) {
                    // 客户端已断开
                }
                emitter.complete();
            }
        });
        return emitter;
    }

    @Operation(summary = "图文 HTML 成品预览")
    @GetMapping("/{id}/preview")
    public R<String> preview(@PathVariable Long id) {
        return R.ok(pipelineService.previewHtml(id));
    }

    @Operation(summary = "保存到内容中心")
    @PostMapping("/{id}/save")
    public R<Long> save(@PathVariable Long id) {
        return R.ok(pipelineService.saveToContent(id));
    }
}
