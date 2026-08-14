package com.dayan.agent.controller.agent;

import com.dayan.agent.dto.AgentLeadCreateDTO;
import com.dayan.agent.dto.AgentLeadQueryDTO;
import com.dayan.agent.dto.AgentLeadUpdateDTO;
import com.dayan.agent.service.AgentLeadService;
import com.dayan.agent.vo.AgentLeadVO;
import com.dayan.common.core.resp.PageResult;
import com.dayan.common.core.resp.R;
import com.dayan.common.log.operation.OperationLog;
import com.dayan.lead.vo.LeadInfoVO;
import com.dayan.lead.vo.LeadTraceVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Agent 代理人端线索接口。
 *
 * <p>路径：{@code /agent-api/leads}（由 dayan-agent 启动模块的 context-path 拼接）。
 * 所有查询/操作自动限定为当前登录代理人的线索。
 *
 * <p>线索池（{@code /leads/pool}）来自 lead 域 lead_info：访客由分享追踪自动建档，
 * 代理人认领后才生成自己的 agent_lead CRM 线索。
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

    @Operation(summary = "线索池分页（本渠道未被认领的访客线索）")
    @GetMapping("/pool")
    public R<PageResult<LeadInfoVO>> pool(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean onlyWithPhone,
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size) {
        return R.ok(agentLeadService.pagePool(keyword, onlyWithPhone, current, size));
    }

    @Operation(summary = "认领线索池线索")
    @OperationLog(module = "线索", action = "认领")
    @PostMapping("/claim/{visitorLeadCode}")
    public R<Long> claim(@PathVariable String visitorLeadCode) {
        return R.ok(agentLeadService.claim(visitorLeadCode));
    }

    @Operation(summary = "线索详情")
    @GetMapping("/{leadId}")
    public R<AgentLeadVO> detail(@PathVariable Long leadId) {
        return R.ok(agentLeadService.detail(leadId));
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

    @Operation(summary = "删除线索")
    @OperationLog(module = "线索", action = "删除")
    @DeleteMapping("/{leadId}")
    public R<Void> delete(@PathVariable Long leadId) {
        agentLeadService.delete(leadId);
        return R.ok();
    }

    @Operation(summary = "线索互动记录（互动时间线，来自 lead 域记录表）")
    @GetMapping("/{leadId}/traces")
    public R<List<LeadTraceVO>> traces(@PathVariable Long leadId) {
        return R.ok(agentLeadService.traces(leadId));
    }
}
