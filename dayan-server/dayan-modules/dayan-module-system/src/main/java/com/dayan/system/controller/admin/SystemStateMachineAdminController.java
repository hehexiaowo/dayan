package com.dayan.system.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.dayan.common.core.resp.PageResult;
import com.dayan.common.core.resp.R;
import com.dayan.common.log.operation.OperationLog;
import com.dayan.system.entity.SystemStateMachine;
import com.dayan.system.service.SystemStateMachineService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * Admin 端状态机规则配置接口。
 */
@Tag(name = "状态机配置")
@RestController
@RequestMapping("/state-machines")
@RequiredArgsConstructor
public class SystemStateMachineAdminController {

    private final SystemStateMachineService stateMachineService;

    @Operation(summary = "状态机规则分页")
    @SaCheckPermission("system:sm:list")
    @GetMapping
    public R<PageResult<SystemStateMachine>> page(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "20") long size,
            @RequestParam(required = false) String machineCode,
            @RequestParam(required = false) String bizType) {
        return R.ok(stateMachineService.page(current, size, machineCode, bizType));
    }

    @Operation(summary = "状态机规则详情")
    @SaCheckPermission("system:sm:list")
    @GetMapping("/{id}")
    public R<SystemStateMachine> getById(@PathVariable Long id) {
        return R.ok(stateMachineService.getById(id));
    }

    @Operation(summary = "新增状态机规则")
    @OperationLog(module = "状态规则", action = "新增")
    @SaCheckPermission("system:sm:create")
    @PostMapping
    public R<Long> create(@RequestBody SystemStateMachine entity) {
        return R.ok(stateMachineService.create(entity));
    }

    @Operation(summary = "修改状态机规则")
    @OperationLog(module = "状态规则", action = "修改")
    @SaCheckPermission("system:sm:update")
    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @RequestBody SystemStateMachine entity) {
        stateMachineService.update(id, entity);
        return R.ok();
    }

    @Operation(summary = "删除状态机规则")
    @OperationLog(module = "状态规则", action = "删除")
    @SaCheckPermission("system:sm:delete")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        stateMachineService.delete(id);
        return R.ok();
    }
}
