package com.dayan.system.controller.agent;

import com.dayan.common.core.resp.R;
import com.dayan.system.entity.SystemDictRegion;
import com.dayan.system.service.RegionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Agent 端行政区划查询接口（省市区三级联动）。
 *
 * <p>公开数据无权限注解；路径 /agent-api/regions/*。
 */
@Tag(name = "Agent 端-行政区划")
@RestController
@RequestMapping("/regions")
@RequiredArgsConstructor
public class RegionAgentController {

    private final RegionService regionService;

    @Operation(summary = "省级行政区列表")
    @GetMapping("/provinces")
    public R<List<SystemDictRegion>> provinces() {
        return R.ok(regionService.listProvinces());
    }

    @Operation(summary = "下级行政区列表（按父级编码）")
    @GetMapping("/children")
    public R<List<SystemDictRegion>> children(@RequestParam String parentCode) {
        return R.ok(regionService.listChildren(parentCode));
    }
}
