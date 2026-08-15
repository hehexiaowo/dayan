package com.dayan.system.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.dayan.common.core.resp.PageResult;
import com.dayan.common.core.resp.R;
import com.dayan.system.entity.SystemMessage;
import com.dayan.system.service.SystemMessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * Admin 端消息发送记录审计接口（只读）。
 *
 * <p>路径 /admin-api/messages。记录由发送链路落库，管理端仅查询排障。
 */
@Tag(name = "消息发送记录")
@RestController
@RequestMapping("/messages")
@RequiredArgsConstructor
public class SystemMessageAdminController {

    private final SystemMessageService messageService;

    @Operation(summary = "发送记录分页")
    @SaCheckPermission("system:msg-record:list")
    @GetMapping
    public R<PageResult<SystemMessage>> page(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "20") long size,
            @RequestParam(required = false) String bizType,
            @RequestParam(required = false) Integer channelType,
            @RequestParam(required = false) Integer sendStatus,
            @RequestParam(required = false) String targetType,
            @RequestParam(required = false) String templateCode,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) {
        return R.ok(messageService.page(current, size, bizType, channelType, sendStatus,
                targetType, templateCode, startTime, endTime));
    }

    @Operation(summary = "发送记录详情")
    @SaCheckPermission("system:msg-record:query")
    @GetMapping("/{id}")
    public R<SystemMessage> getById(@PathVariable Long id) {
        return R.ok(messageService.getById(id));
    }
}
