package com.dayan.agent.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.dayan.agent.dto.AgentPerformanceCreateDTO;
import com.dayan.agent.dto.AgentPerformanceQueryDTO;
import com.dayan.agent.service.AgentPerformanceService;
import com.dayan.agent.vo.AgentPerformanceSummaryVO;
import com.dayan.agent.vo.AgentPerformanceVO;
import com.dayan.common.core.resp.PageResult;
import com.dayan.common.core.resp.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Admin 端代理人业绩接口。
 */
@Tag(name = "代理人业绩管理")
@RestController
@RequestMapping("/agent-performances")
@RequiredArgsConstructor
public class AgentPerformanceAdminController {

    private final AgentPerformanceService agentPerformanceService;

    @Operation(summary = "业绩分页列表")
    @SaCheckPermission("agent:performance:list")
    @GetMapping
    public R<PageResult<AgentPerformanceVO>> page(AgentPerformanceQueryDTO query) {
        return R.ok(agentPerformanceService.page(query));
    }

    @Operation(summary = "新增业绩")
    @SaCheckPermission("agent:performance:create")
    @PostMapping
    public R<Void> create(@RequestBody @Valid AgentPerformanceCreateDTO dto) {
        agentPerformanceService.create(dto);
        return R.ok();
    }

    @Operation(summary = "查代理人业绩列表")
    @SaCheckPermission("agent:performance:list")
    @GetMapping("/by-agent/{agentCode}")
    public R<List<AgentPerformanceVO>> listByAgent(@PathVariable String agentCode) {
        return R.ok(agentPerformanceService.listByAgent(agentCode));
    }

    @Operation(summary = "按代理人汇总业绩")
    @SaCheckPermission("agent:performance:summary")
    @GetMapping("/summary/{agentCode}")
    public R<AgentPerformanceSummaryVO> summary(@PathVariable String agentCode) {
        return R.ok(agentPerformanceService.summary(agentCode));
    }
}
