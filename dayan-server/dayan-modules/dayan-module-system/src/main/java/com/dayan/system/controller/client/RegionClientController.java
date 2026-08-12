package com.dayan.system.controller.client;

import com.dayan.common.core.resp.R;
import com.dayan.system.entity.SystemDictRegion;
import com.dayan.system.service.RegionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Client 端行政区划查询接口（省市区三级联动）。
 *
 * <p>公开数据无权限注解；路径 /client-api/regions/*。
 * 逻辑与 {@link com.dayan.system.controller.agent.RegionAgentController} 一致。
 */
@Tag(name = "Client 端-行政区划")
@RestController
@RequestMapping("/regions")
@RequiredArgsConstructor
public class RegionClientController {

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
