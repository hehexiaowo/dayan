package com.dayan.channel.controller.channel;

import com.dayan.channel.service.ChannelDashboardService;
import com.dayan.channel.vo.DashboardStatsVO;
import com.dayan.common.core.resp.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Channel 渠道端工作台接口。
 *
 * <p>路径：{@code /channel-api/dashboard/*}（由 dayan-channel 启动模块的 context-path 拼接）。
 */
@Tag(name = "Channel 工作台")
@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class ChannelDashboardController {

    private final ChannelDashboardService dashboardService;

    @Operation(summary = "工作台汇总统计")
    @GetMapping("/stats")
    public R<DashboardStatsVO> stats() {
        return R.ok(dashboardService.getStats());
    }
}
