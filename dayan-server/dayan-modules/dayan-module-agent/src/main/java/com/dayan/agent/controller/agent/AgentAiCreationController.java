package com.dayan.agent.controller.agent;

import com.dayan.agent.dto.AiOutlineConfirmDTO;
import com.dayan.agent.dto.AiOutlineRegenDTO;
import com.dayan.agent.dto.AiProjectCreateDTO;
import com.dayan.agent.dto.AiStrategyConfirmDTO;
import com.dayan.agent.dto.AiTitleRegenDTO;
import com.dayan.agent.service.AiCreationPipelineService;
import com.dayan.agent.service.AiCreationProjectService;
import com.dayan.common.core.resp.PageResult;
import com.dayan.common.core.resp.R;
import com.dayan.agent.vo.AiProjectListVO;
import com.dayan.agent.vo.AiProjectVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

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
}
