package com.dayan.agent.controller.agent;

import com.dayan.agent.dto.AgentContentCreateDTO;
import com.dayan.agent.dto.AgentContentQueryDTO;
import com.dayan.agent.dto.AgentContentUpdateDTO;
import com.dayan.agent.dto.AiConvertDTO;
import com.dayan.agent.dto.AiTopicsDTO;
import com.dayan.agent.service.AgentContentService;
import com.dayan.agent.service.AiContentGenerateService;
import com.dayan.agent.vo.AgentContentVO;
import com.dayan.agent.vo.AiGenerateResultVO;
import com.dayan.common.core.resp.PageResult;
import com.dayan.common.core.resp.R;
import com.dayan.agent.model.AiRefTemplates;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Agent AI 内容接口：选题灵感 + 形态转换（轻量，不落项目）+ 个人内容 CRUD。
 *
 * <p>单次生成（generate/generate/stream）已由六阶段流水线
 * {@link AgentAiCreationController} 取代并下线。
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

    @Operation(summary = "选题灵感（基于勾选素材 + 时节出 5 个获客选题）")
    @PostMapping("/topics")
    public R<List<String>> topics(@RequestBody AiTopicsDTO dto) {
        return R.ok(aiContentGenerateService.suggestTopics(dto));
    }

    @Operation(summary = "形态转换（已生成内容改写为其他发布形态，事实保持一致）")
    @PostMapping("/convert")
    public R<AiGenerateResultVO> convert(@RequestBody @Valid AiConvertDTO dto) {
        return R.ok(aiContentGenerateService.convert(dto));
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
