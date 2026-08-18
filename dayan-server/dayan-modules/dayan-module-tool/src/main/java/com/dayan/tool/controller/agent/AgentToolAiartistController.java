package com.dayan.tool.controller.agent;

import com.dayan.common.core.resp.PageResult;
import com.dayan.common.core.resp.R;
import com.dayan.common.log.operation.OperationLog;
import com.dayan.tool.dto.AiOutlineConfirmDTO;
import com.dayan.tool.dto.AiOutlineRegenDTO;
import com.dayan.tool.dto.AiReviseDTO;
import com.dayan.tool.dto.AiStrategyConfirmDTO;
import com.dayan.tool.dto.AiTitleRegenDTO;
import com.dayan.tool.dto.ToolAiartistCreateDTO;
import com.dayan.tool.dto.ToolAiartistQueryDTO;
import com.dayan.tool.model.AiRefTemplates;
import com.dayan.tool.service.ToolAiartistPipelineService;
import com.dayan.tool.service.ToolAiartistService;
import com.dayan.tool.service.ToolAiartistStreamService;
import com.dayan.tool.service.ToolInfoService;
import com.dayan.tool.vo.ToolAiartistConfigVO;
import com.dayan.tool.vo.ToolAiartistListVO;
import com.dayan.tool.vo.ToolAiartistVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

/**
 * Agent AI 创作六阶段流水线。路径 {@code /agent-api/tools/ai-creator/...}。
 * agentCode 服务端从登录上下文注入，防越权；写操作记操作日志。
 */
@Tag(name = "Agent AI 创作")
@RestController
@RequestMapping("/tools/ai-creator")
@RequiredArgsConstructor
public class AgentToolAiartistController {

    private final ToolAiartistService projectService;
    private final ToolAiartistPipelineService pipelineService;
    private final ToolAiartistStreamService streamService;
    private final ToolInfoService toolInfoService;

    @Operation(summary = "创作分类列表（tool_type=aiartist 且启用）")
    @GetMapping("/configs")
    public R<List<ToolAiartistConfigVO>> configs() {
        return R.ok(toolInfoService.listAiartistConfigs());
    }

    @Operation(summary = "创建创作项目")
    @OperationLog(module = "AI 创作", action = "创建项目")
    @PostMapping
    public R<Long> create(@RequestBody @Valid ToolAiartistCreateDTO dto) {
        return R.ok(projectService.create(dto));
    }

    @Operation(summary = "我的创作列表")
    @GetMapping("/list")
    public R<PageResult<ToolAiartistListVO>> list(ToolAiartistQueryDTO dto) {
        return R.ok(projectService.page(dto));
    }

    @Operation(summary = "项目详情（恢复草稿）")
    @GetMapping("/{id}")
    public R<ToolAiartistVO> detail(@PathVariable Long id) {
        return R.ok(projectService.getDetail(id));
    }

    @Operation(summary = "删除草稿")
    @OperationLog(module = "AI 创作", action = "删除草稿")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        projectService.delete(id);
        return R.ok();
    }

    @Operation(summary = "重跑素材消化")
    @PostMapping("/{id}/digest")
    public R<ToolAiartistVO> digest(@PathVariable Long id) {
        return R.ok(pipelineService.digest(id));
    }

    @Operation(summary = "生成策略+5标题")
    @PostMapping("/{id}/strategy")
    public R<ToolAiartistVO> strategy(@PathVariable Long id) {
        return R.ok(pipelineService.strategy(id));
    }

    @Operation(summary = "带反馈重出标题（策略锁定）")
    @PostMapping("/{id}/titles/regenerate")
    public R<ToolAiartistVO> regenTitles(@PathVariable Long id, @RequestBody(required = false) AiTitleRegenDTO dto) {
        return R.ok(pipelineService.regenerateTitles(id, dto));
    }

    @Operation(summary = "锁定策略+选定标题")
    @OperationLog(module = "AI 创作", action = "锁定策略")
    @PostMapping("/{id}/strategy/confirm")
    public R<ToolAiartistVO> confirmStrategy(@PathVariable Long id, @RequestBody @Valid AiStrategyConfirmDTO dto) {
        return R.ok(pipelineService.confirmStrategy(id, dto));
    }

    @Operation(summary = "生成大纲")
    @PostMapping("/{id}/outline")
    public R<ToolAiartistVO> outline(@PathVariable Long id) {
        return R.ok(pipelineService.outline(id));
    }

    @Operation(summary = "带反馈重生成大纲")
    @PostMapping("/{id}/outline/regenerate")
    public R<ToolAiartistVO> regenOutline(@PathVariable Long id, @RequestBody(required = false) AiOutlineRegenDTO dto) {
        return R.ok(pipelineService.regenerateOutline(id, dto));
    }

    @Operation(summary = "锁定大纲（可含微调）")
    @OperationLog(module = "AI 创作", action = "锁定大纲")
    @PostMapping("/{id}/outline/confirm")
    public R<ToolAiartistVO> confirmOutline(@PathVariable Long id, @RequestBody @Valid AiOutlineConfirmDTO dto) {
        return R.ok(pipelineService.confirmOutline(id, dto));
    }

    @Operation(summary = "生成正文（非流式，小程序降级用）")
    @PostMapping("/{id}/body")
    public R<ToolAiartistVO> body(@PathVariable Long id) {
        return R.ok(pipelineService.bodyStream(id, null));
    }

    @Operation(summary = "生成正文（SSE：body→audit→polish）")
    @PostMapping("/{id}/body/stream")
    public SseEmitter bodyStream(@PathVariable Long id) {
        return streamService.bodyStream(id);
    }

    @Operation(summary = "段落勘误（最小化修订）")
    @PostMapping("/{id}/revise")
    public R<ToolAiartistVO> revise(@PathVariable Long id, @RequestBody @Valid AiReviseDTO dto) {
        return R.ok(pipelineService.revise(id, dto));
    }

    @Operation(summary = "生成配图（SSE：逐张进度）")
    @PostMapping("/{id}/images/stream")
    public SseEmitter imagesStream(@PathVariable Long id) {
        return streamService.imagesStream(id);
    }

    @Operation(summary = "图文 HTML 成品预览")
    @GetMapping("/{id}/preview")
    public R<String> preview(@PathVariable Long id) {
        return R.ok(pipelineService.previewHtml(id));
    }

    @Operation(summary = "保存到内容中心")
    @OperationLog(module = "AI 创作", action = "保存到内容中心")
    @PostMapping("/{id}/save")
    public R<Long> save(@PathVariable Long id) {
        return R.ok(pipelineService.saveToContent(id));
    }

    @Operation(summary = "参考范文模板")
    @GetMapping("/templates")
    public R<List<AiRefTemplates.RefTemplate>> templates() {
        return R.ok(AiRefTemplates.TEMPLATES);
    }
}
