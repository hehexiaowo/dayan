package com.dayan.system.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.dayan.common.core.resp.R;
import com.dayan.system.entity.SystemDictRegion;
import com.dayan.system.service.RegionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Admin 端行政区划查询接口。
 *
 * <p>供前端省市区三级联动下拉使用。权限复用 system:dict:list（地域是字典的一种）。
 */
@Tag(name = "行政区划")
@RestController
@RequestMapping("/regions")
@RequiredArgsConstructor
public class RegionController {

    private final RegionService regionService;

    @Operation(summary = "省级行政区列表")
    @SaCheckPermission("system:dict:list")
    @GetMapping("/provinces")
    public R<List<SystemDictRegion>> provinces() {
        return R.ok(regionService.listProvinces());
    }

    @Operation(summary = "下级行政区列表（按父级编码）")
    @SaCheckPermission("system:dict:list")
    @GetMapping("/children")
    public R<List<SystemDictRegion>> children(@RequestParam String parentCode) {
        return R.ok(regionService.listChildren(parentCode));
    }
}
