package com.dayan.system.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.dayan.common.core.resp.PageResult;
import com.dayan.common.core.resp.R;
import com.dayan.common.log.operation.OperationLog;
import com.dayan.system.entity.SystemConfig;
import com.dayan.system.service.SystemConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Admin 端系统配置接口。
 */
@Tag(name = "系统配置")
@RestController
@RequestMapping("/configs")
@RequiredArgsConstructor
public class SystemConfigAdminController {

    private final SystemConfigService configService;

    @Operation(summary = "配置分页")
    @SaCheckPermission("system:config:list")
    @GetMapping
    public R<PageResult<SystemConfig>> page(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "20") long size,
            @RequestParam(required = false) String configGroup,
            @RequestParam(required = false) String configKey) {
        return R.ok(configService.page(current, size, configGroup, configKey));
    }

    @Operation(summary = "按分组查询配置")
    @SaCheckPermission("system:config:list")
    @GetMapping("/group/{configGroup}")
    public R<List<SystemConfig>> listByGroup(@PathVariable String configGroup) {
        return R.ok(configService.listByGroup(configGroup));
    }

    @Operation(summary = "新增配置")
    @OperationLog(module = "系统配置", action = "新增")
    @SaCheckPermission("system:config:create")
    @PostMapping
    public R<String> create(@RequestBody SystemConfig config) {
        return R.ok(configService.create(config));
    }

    @Operation(summary = "修改配置")
    @OperationLog(module = "系统配置", action = "修改")
    @SaCheckPermission("system:config:update")
    @PutMapping("/{configKey}")
    public R<Void> update(@PathVariable String configKey, @RequestBody SystemConfig config) {
        configService.update(configKey, config);
        return R.ok();
    }

    @Operation(summary = "删除配置")
    @OperationLog(module = "系统配置", action = "删除")
    @SaCheckPermission("system:config:delete")
    @DeleteMapping("/{configKey}")
    public R<Void> delete(@PathVariable String configKey) {
        configService.delete(configKey);
        return R.ok();
    }
}
