package com.dayan.park.controller.agent;

import com.dayan.common.core.resp.R;
import com.dayan.park.dto.RegionQueryDTO;
import com.dayan.park.service.ParkAgentQueryService;
import com.dayan.park.vo.CategoryCountVO;
import com.dayan.park.vo.ParkInfoVO;
import com.dayan.park.vo.RegionDrillResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Agent 端-机构查询 Controller。
 *
 * <p>被 dayan-agent 启动器加载（ComponentScan 保留 controller/agent 包）。
 * 鉴权靠 Sa-Token 登录态拦截（Agent-Token 头），不用 @SaCheckPermission。
 * park_info 是平台共享表，不需 channel_code 隔离。
 *
 * <p>所有查询都强制 is_published=1 + operate_status=1 + deleted=0，
 * agent 端只能看到已上线的机构。
 */
@Tag(name = "Agent 端-机构查询")
@Validated
@RestController
@RequestMapping("/park")
@RequiredArgsConstructor
public class ParkAgentController {

    private final ParkAgentQueryService parkAgentQueryService;

    /**
     * 三分类机构数量统计（分类入口页用）。
     * GET /agent-api/park/categories
     */
    @Operation(summary = "三分类机构数量统计")
    @GetMapping("/categories")
    public R<List<CategoryCountVO>> categories() {
        return R.ok(parkAgentQueryService.countByCategory());
    }

    /**
     * 区域下钻查询（省/市/区/机构清单通用）。
     * GET /agent-api/park/regions?category=vital&level=province
     * GET /agent-api/park/regions?category=vital&level=city&provinceCode=110000
     */
    @Operation(summary = "区域下钻查询")
    @GetMapping("/regions")
    public R<RegionDrillResult> regions(@Validated RegionQueryDTO query) {
        return R.ok(parkAgentQueryService.drillRegion(query));
    }

    /**
     * 机构详情（仅返回已发布+已上线机构）。
     * GET /agent-api/park/{parkCode}
     */
    @Operation(summary = "机构详情")
    @GetMapping("/{parkCode}")
    public R<ParkInfoVO> detail(@PathVariable String parkCode) {
        return R.ok(parkAgentQueryService.getPublishedDetail(parkCode));
    }
}
