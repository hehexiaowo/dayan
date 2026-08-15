package com.dayan.system.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.dayan.common.core.resp.PageResult;
import com.dayan.common.core.resp.R;
import com.dayan.common.log.operation.OperationLog;
import com.dayan.system.entity.SystemMessageTemplate;
import com.dayan.system.service.SystemMessageTemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * Admin 端消息模板管理接口。
 *
 * <p>路径 /admin-api/message-templates。模板编码被发送记录引用，更新不可改码。
 */
@Tag(name = "消息模板")
@RestController
@RequestMapping("/message-templates")
@RequiredArgsConstructor
public class SystemMessageTemplateAdminController {

    private final SystemMessageTemplateService templateService;

    @Operation(summary = "消息模板分页")
    @SaCheckPermission("system:msg-tpl:list")
    @GetMapping
    public R<PageResult<SystemMessageTemplate>> page(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "20") long size,
            @RequestParam(required = false) String templateCode,
            @RequestParam(required = false) String templateName,
            @RequestParam(required = false) String bizType,
            @RequestParam(required = false) Integer channelType,
            @RequestParam(required = false) Integer status) {
        return R.ok(templateService.page(current, size, templateCode, templateName, bizType, channelType, status));
    }

    @Operation(summary = "新增消息模板")
    @OperationLog(module = "消息模板", action = "新增")
    @SaCheckPermission("system:msg-tpl:create")
    @PostMapping
    public R<Long> create(@RequestBody SystemMessageTemplate template) {
        return R.ok(templateService.create(template));
    }

    @Operation(summary = "修改消息模板")
    @OperationLog(module = "消息模板", action = "修改")
    @SaCheckPermission("system:msg-tpl:update")
    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @RequestBody SystemMessageTemplate template) {
        templateService.update(id, template);
        return R.ok();
    }

    @Operation(summary = "删除消息模板")
    @OperationLog(module = "消息模板", action = "删除")
    @SaCheckPermission("system:msg-tpl:delete")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        templateService.delete(id);
        return R.ok();
    }
}
