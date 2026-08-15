package com.dayan.agent.controller.agent;

import com.dayan.agent.dto.AgentContentCreateDTO;
import com.dayan.agent.dto.AgentContentQueryDTO;
import com.dayan.agent.dto.AgentContentUpdateDTO;
import com.dayan.agent.service.AgentContentService;
import com.dayan.agent.vo.AgentContentVO;
import com.dayan.common.core.resp.PageResult;
import com.dayan.common.core.resp.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * Agent AI 内容接口：生成（不落库）+ 个人内容 CRUD。
 *
 * <p>路径 {@code /agent-api/ai/...}。agentCode 服务端从登录上下文注入，防越权。
 */
@Tag(name = "Agent AI 内容")
@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
public class AgentAiContentController {

    private final AgentContentService agentContentService;

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
    public R<Void> update(@PathVariable Long id, @RequestBody AgentContentUpdateDTO dto) {
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
