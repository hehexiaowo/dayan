package com.dayan.park.controller.client;

import com.dayan.common.core.enums.NetworkType;
import com.dayan.common.core.exception.BusinessException;
import com.dayan.common.core.exception.ErrorCode;
import com.dayan.common.core.resp.R;
import com.dayan.park.dto.RegionQueryDTO;
import com.dayan.park.service.ParkAgentQueryService;
import com.dayan.park.vo.CategoryCountVO;
import com.dayan.park.vo.ParkFullDetailVO;
import com.dayan.park.vo.ParkInfoVO;
import com.dayan.park.vo.RegionDrillResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Client 端-机构查询 Controller。
 *
 * <p>被 dayan-client 启动器加载（ComponentScan 保留 controller/client 包，
 * DayanClientApplication 的 excludeFilters 排除 admin/agent/channel 等端）。
 *
 * <p>与 {@link com.dayan.park.controller.agent.ParkAgentController} 镜像，
 * 复用同一个 {@link ParkAgentQueryService}（只读，无端差异化逻辑）。
 * park_info 是平台共享表，不需 channel_code 隔离。
 *
 * <p>所有查询都强制 is_published=1 + operate_status=1 + deleted=0，
 * client 端只能看到已上线、对外的机构。context-path /client-api 使
 * 实际 URL 为 /client-api/park/*，与 agent 端 /agent-api/park/* 隔离。
 */
@Tag(name = "Client 端-机构查询")
@Validated
@RestController
@RequestMapping("/park")
@RequiredArgsConstructor
public class ParkClientController {

    private final ParkAgentQueryService parkAgentQueryService;

    /**
     * 三分类机构数量统计（分类入口页用）。
     * GET /client-api/park/categories
     */
    @Operation(summary = "三分类机构数量统计")
    @GetMapping("/categories")
    public R<List<CategoryCountVO>> categories() {
        return R.ok(parkAgentQueryService.countByCategory());
    }

    /**
     * 区域下钻查询（省/市/区/机构清单通用）。
     * GET /client-api/park/regions?category=vital&level=province
     * GET /client-api/park/regions?category=vital&level=city&provinceCode=110000
     */
    @Operation(summary = "区域下钻查询")
    @GetMapping("/regions")
    public R<RegionDrillResult> regions(@Validated RegionQueryDTO query) {
        return R.ok(parkAgentQueryService.drillRegion(query));
    }

    /**
     * 机构详情（仅返回已发布+已上线机构）。
     * GET /client-api/park/{parkCode}
     */
    @Operation(summary = "机构详情")
    @GetMapping("/{parkCode}")
    public R<ParkInfoVO> detail(@PathVariable String parkCode) {
        return R.ok(parkAgentQueryService.getPublishedDetail(parkCode));
    }

    /**
     * 机构完整详情（主表 + 全部子实体聚合，供详情页 Tab 展示）。
     * GET /client-api/park/{parkCode}/full?network=vital
     */
    @Operation(summary = "机构完整详情（聚合，可按业态过滤板块）")
    @GetMapping("/{parkCode}/full")
    public R<ParkFullDetailVO> fullDetail(@PathVariable String parkCode,
            @RequestParam(value = "network", required = false) String network) {
        if (network != null && !network.isBlank() && NetworkType.of(network) == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "非法业态参数: " + network);
        }
        return R.ok(parkAgentQueryService.getFullDetail(parkCode, network));
    }
}
