package com.dayan.lead.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.dayan.common.core.resp.PageResult;
import com.dayan.common.core.resp.R;
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
 * Admin 端线索池管理接口。
 *
 * <p>路径：{@code /lead/info/*}（context-path 拼接为 {@code /admin-api/lead/info/*}）。
 * 线索只读域：由分享追踪自动建档，后台仅提供检索/详情/时间线，不提供新增改删。
 */
@Tag(name = "线索池管理")
@RestController
@RequestMapping("/lead/info")
@RequiredArgsConstructor
public class LeadInfoAdminController {

    private final LeadInfoService leadInfoService;
    private final LeadTrackService leadTrackService;

    @Operation(summary = "线索分页列表")
    @SaCheckPermission("lead:info:list")
    @GetMapping("/page")
    public R<PageResult<LeadInfoVO>> page(
            @RequestParam(required = false) String channelCode,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean onlyWithPhone,
            @RequestParam(required = false) Boolean excludeClaimed,
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size) {
        return R.ok(leadInfoService.page(channelCode, keyword, onlyWithPhone,
                Boolean.TRUE.equals(excludeClaimed), current, size));
    }

    @Operation(summary = "线索详情")
    @SaCheckPermission("lead:info:query")
    @GetMapping("/{leadCode}")
    public R<LeadInfoVO> detail(@PathVariable String leadCode) {
        return R.ok(leadInfoService.getByLeadCode(leadCode));
    }

    @Operation(summary = "线索互动时间线（内容/工具/海报合并视图）")
    @SaCheckPermission("lead:info:query")
    @GetMapping("/{leadCode}/traces")
    public R<List<LeadTraceVO>> traces(@PathVariable String leadCode) {
        LeadInfoVO lead = leadInfoService.getByLeadCode(leadCode);
        return R.ok(leadTrackService.listTraces(lead.getVisitorToken(), 100));
    }
}
