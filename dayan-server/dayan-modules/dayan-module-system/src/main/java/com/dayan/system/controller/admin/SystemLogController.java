package com.dayan.system.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.dayan.common.core.resp.PageResult;
import com.dayan.common.core.resp.R;
import com.dayan.system.entity.SystemLogEntry;
import com.dayan.system.service.SystemLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * Admin 端系统日志审计接口（四端分表）。
 *
 * <p>提供 system_log_organ / system_log_channel / system_log_agent / system_log_client
 * 四表的分页查询与详情查询，供运营审计页按来源切换展示。
 * 日志由 {@code @OperationLog} 切面（操作）与 {@code AuthLogRecorder}（登录/登出）异步落库。
 */
@Tag(name = "系统日志")
@RestController
@RequestMapping("/logs")
@RequiredArgsConstructor
public class SystemLogController {

    private final SystemLogService systemLogService;

    @Operation(summary = "系统日志分页")
    @SaCheckPermission("system:log:list")
    @GetMapping
    public R<PageResult<? extends SystemLogEntry>> page(
            @RequestParam(defaultValue = "organ") String source,
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "20") long size,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String accountCode,
            @RequestParam(required = false) Integer resultStatus,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) {
        return R.ok(systemLogService.page(source, current, size, module, accountCode, resultStatus, startTime, endTime));
    }

    @Operation(summary = "系统日志详情")
    @SaCheckPermission("system:log:list")
    @GetMapping("/{id}")
    public R<SystemLogEntry> getById(@PathVariable Long id,
                                     @RequestParam(defaultValue = "organ") String source) {
        return R.ok(systemLogService.getById(source, id));
    }
}
