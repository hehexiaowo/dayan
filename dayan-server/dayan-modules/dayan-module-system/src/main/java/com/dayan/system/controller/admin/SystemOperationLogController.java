package com.dayan.system.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.dayan.common.core.resp.PageResult;
import com.dayan.common.core.resp.R;
import com.dayan.system.entity.SystemOperationLog;
import com.dayan.system.service.SystemOperationLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * Admin 端操作日志审计接口。
 *
 * <p>提供 system_operation_log 表的分页查询与详情查询，供运营审计页展示。
 * 日志由 {@code @OperationLog} 注解 + {@code OperationLogAspect} 切面异步落库。
 */
@Tag(name = "操作日志")
@RestController
@RequestMapping("/operation-logs")
@RequiredArgsConstructor
public class SystemOperationLogController {

    private final SystemOperationLogService operationLogService;

    @Operation(summary = "操作日志分页")
    @SaCheckPermission("system:log:list")
    @GetMapping
    public R<PageResult<SystemOperationLog>> page(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "20") long size,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String accountCode,
            @RequestParam(required = false) Integer resultStatus,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) {
        return R.ok(operationLogService.page(current, size, module, accountCode, resultStatus, startTime, endTime));
    }

    @Operation(summary = "操作日志详情")
    @SaCheckPermission("system:log:list")
    @GetMapping("/{id}")
    public R<SystemOperationLog> getById(@PathVariable Long id) {
        return R.ok(operationLogService.getById(id));
    }
}
