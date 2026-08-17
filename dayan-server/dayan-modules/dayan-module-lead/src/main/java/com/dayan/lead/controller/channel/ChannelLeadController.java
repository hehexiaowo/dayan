package com.dayan.lead.controller.channel;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.dayan.common.core.resp.PageResult;
import com.dayan.common.core.resp.R;
import com.dayan.common.mybatis.context.ContextHolder;
import com.dayan.lead.service.LeadInfoService;
import com.dayan.lead.service.LeadTrackService;
import com.dayan.lead.vo.LeadInfoVO;
import com.dayan.lead.vo.LeadTraceVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Channel 渠道端客户线索接口（渠道视角的线索池）。
 *
 * <p>路径：{@code /channel-api/leads}。
 * 渠道隔离：channelCode 强制取当前登录上下文（忽略入参，防跨渠道查询），
 * 且 lead_info 表有 channel_code 列，租户拦截器会二次兜底注入渠道条件。
 * 权限码沿用现有「客户线索」菜单绑定的 {@code channel:agentClient:list}。
 */
@Tag(name = "Channel 客户线索")
@RestController
@RequestMapping("/leads")
@RequiredArgsConstructor
public class ChannelLeadController {

    private final LeadInfoService leadInfoService;
    private final LeadTrackService leadTrackService;

    @Operation(summary = "本渠道线索分页列表")
    @SaCheckPermission("channel:agentClient:list")
    @GetMapping
    public R<PageResult<LeadInfoVO>> page(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean onlyWithPhone,
            @RequestParam(required = false) Boolean excludeClaimed,
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size) {
        String channelCode = ContextHolder.getChannelCode();
        return R.ok(leadInfoService.page(channelCode, keyword, onlyWithPhone,
                Boolean.TRUE.equals(excludeClaimed), current, size));
    }

    @Operation(summary = "线索互动时间线（内容/工具/海报合并视图）")
    @SaCheckPermission("channel:agentClient:list")
    @GetMapping("/{leadCode}/traces")
    public R<List<LeadTraceVO>> traces(@PathVariable String leadCode) {
        LeadInfoVO lead = leadInfoService.getByLeadCode(leadCode);
        // 渠道防越权：线索归属校验（非本渠道线索拒绝查看时间线）
        String channelCode = ContextHolder.getChannelCode();
        if (lead == null || channelCode == null || !channelCode.equals(lead.getChannelCode())) {
            return R.fail(com.dayan.common.core.exception.ErrorCode.FORBIDDEN.getCode(), "无权查看该线索");
        }
        return R.ok(leadTrackService.listTraces(lead.getVisitorToken(), 100));
    }
}
