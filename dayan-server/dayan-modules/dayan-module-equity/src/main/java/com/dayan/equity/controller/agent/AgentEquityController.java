package com.dayan.equity.controller.agent;

import com.dayan.common.core.exception.BusinessException;
import com.dayan.common.core.exception.ErrorCode;
import com.dayan.common.core.resp.PageResult;
import com.dayan.common.core.resp.R;
import com.dayan.common.mybatis.context.ContextHolder;
import com.dayan.equity.dto.EquityDepotQueryDTO;
import com.dayan.equity.service.EquityDepotService;
import com.dayan.equity.vo.AgentEquityStatsVO;
import com.dayan.equity.vo.EquityDepotVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Agent 代理人端权益接口。
 *
 * <p>路径：{@code /agent-api/equities}（由 dayan-agent 启动模块的 context-path 拼接）。
 *
 * <p>防越权：agentCode 从 {@link ContextHolder} 强制注入，不接受前端传入。
 * 只读列表 + 统计查询，不触发权益状态机写链路。
 */
@Tag(name = "Agent 权益")
@RestController
@RequestMapping("/equities")
@RequiredArgsConstructor
public class AgentEquityController {

    private final EquityDepotService equityDepotService;

    @Operation(summary = "我的权益卡列表")
    @GetMapping
    public R<PageResult<EquityDepotVO>> page(EquityDepotQueryDTO query) {
        query.setAgentCode(requireCurrentAgentCode());
        return R.ok(equityDepotService.page(query));
    }

    @Operation(summary = "我的权益卡状态统计")
    @GetMapping("/stats")
    public R<AgentEquityStatsVO> stats() {
        String agentCode = requireCurrentAgentCode();
        Map<Integer, Long> counts = equityDepotService.countByAgentCode(agentCode);

        AgentEquityStatsVO stats = new AgentEquityStatsVO();
        long voided = counts.getOrDefault(6, 0L);  // 已作废不计入 total
        stats.setTotal(counts.values().stream().mapToLong(Long::longValue).sum() - voided);
        stats.setStock(counts.getOrDefault(0, 0L));
        stats.setOutbound(counts.getOrDefault(1, 0L));
        stats.setActivated(counts.getOrDefault(2, 0L));
        stats.setInUse(counts.getOrDefault(3, 0L));
        stats.setCompleted(counts.getOrDefault(4, 0L));
        return R.ok(stats);
    }

    private String requireCurrentAgentCode() {
        String agentCode = ContextHolder.getAccountCode();
        if (!StringUtils.hasText(agentCode)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "请先登录");
        }
        return agentCode;
    }
}
