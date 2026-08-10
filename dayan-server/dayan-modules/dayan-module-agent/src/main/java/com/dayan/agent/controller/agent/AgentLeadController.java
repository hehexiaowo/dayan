package com.dayan.agent.controller.agent;

import com.dayan.agent.dto.AgentLeadCreateDTO;
import com.dayan.agent.dto.AgentLeadQueryDTO;
import com.dayan.agent.dto.AgentLeadUpdateDTO;
import com.dayan.agent.service.AgentLeadService;
import com.dayan.agent.vo.AgentLeadVO;
import com.dayan.common.core.resp.PageResult;
import com.dayan.common.core.resp.R;
import com.dayan.common.log.operation.OperationLog;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * Agent 代理人端线索接口。
 *
 * <p>路径：{@code /agent-api/leads}（由 dayan-agent 启动模块的 context-path 拼接）。
 * 所有查询/操作自动限定为当前登录代理人的线索。
 */
@Tag(name = "Agent 线索")
@RestController
@RequestMapping("/leads")
@RequiredArgsConstructor
public class AgentLeadController {

    private final AgentLeadService agentLeadService;

    @Operation(summary = "分页查询我的线索")
    @GetMapping
    public R<PageResult<AgentLeadVO>> page(AgentLeadQueryDTO query) {
        return R.ok(agentLeadService.page(query));
    }

    @Operation(summary = "新增线索")
    @OperationLog(module = "线索", action = "新增")
    @PostMapping
    public R<Long> create(@RequestBody @Valid AgentLeadCreateDTO dto) {
        return R.ok(agentLeadService.create(dto));
    }

    @Operation(summary = "更新线索（含状态变更）")
    @OperationLog(module = "线索", action = "更新")
    @PutMapping("/{leadId}")
    public R<Void> update(@PathVariable Long leadId,
                          @RequestBody AgentLeadUpdateDTO dto) {
        agentLeadService.update(leadId, dto);
        return R.ok();
    }
}
