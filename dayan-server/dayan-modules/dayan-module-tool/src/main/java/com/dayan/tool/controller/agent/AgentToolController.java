package com.dayan.tool.controller.agent;

import com.dayan.common.core.resp.R;
import com.dayan.tool.service.ToolInfoService;
import com.dayan.tool.vo.ToolInfoVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Agent 端工具接口。
 *
 * <p>路径：{@code /tools}（agent starter context-path 拼接为 {@code /agent-api/tools}）。
 * 仅返回启用且 visibleScope 含 agent 的工具，按 sortOrder 升序。
 */
@Tag(name = "Agent 工具")
@RestController
@RequestMapping("/tools")
@RequiredArgsConstructor
public class AgentToolController {

    private final ToolInfoService toolInfoService;

    @Operation(summary = "端上可用工具列表")
    @GetMapping
    public R<List<ToolInfoVO>> list() {
        return R.ok(toolInfoService.listForEnd("agent"));
    }
}
