package com.dayan.channel.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.dayan.channel.dto.ChannelPermissionCreateDTO;
import com.dayan.channel.dto.ChannelPermissionQueryDTO;
import com.dayan.channel.dto.ChannelPermissionUpdateDTO;
import com.dayan.channel.entity.ChannelPermission;
import com.dayan.channel.service.ChannelPermissionService;
import com.dayan.common.core.resp.PageResult;
import com.dayan.common.core.resp.R;
import com.dayan.common.log.operation.OperationLog;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Admin 端渠道权限管理接口（P2 简化：基础 CRUD）。
 *
 * <p>路径：{@code /channel-permissions/*}。
 */
@Tag(name = "渠道权限管理")
@RestController
@RequestMapping("/channel-permissions")
@RequiredArgsConstructor
public class ChannelPermissionAdminController {

    private final ChannelPermissionService channelPermissionService;

    @Operation(summary = "渠道权限分页列表")
    @SaCheckPermission("channel:permission:list")
    @GetMapping
    public R<PageResult<ChannelPermission>> page(ChannelPermissionQueryDTO query) {
        return R.ok(channelPermissionService.page(query));
    }

    @Operation(summary = "全部启用权限（授权选择用）")
    @SaCheckPermission("channel:permission:query")
    @GetMapping("/all")
    public R<List<ChannelPermission>> listAll() {
        return R.ok(channelPermissionService.listAll());
    }

    @Operation(summary = "渠道权限详情")
    @SaCheckPermission("channel:permission:query")
    @GetMapping("/{permissionCode}")
    public R<ChannelPermission> getDetail(@PathVariable String permissionCode) {
        return R.ok(channelPermissionService.getDetail(permissionCode));
    }

    @Operation(summary = "新增渠道权限")
    @OperationLog(module = "渠道权限", action = "新增")
    @SaCheckPermission("channel:permission:create")
    @PostMapping
    public R<Void> create(@RequestBody @Valid ChannelPermissionCreateDTO dto) {
        channelPermissionService.create(dto);
        return R.ok();
    }

    @Operation(summary = "修改渠道权限")
    @OperationLog(module = "渠道权限", action = "修改")
    @SaCheckPermission("channel:permission:update")
    @PutMapping("/{permissionCode}")
    public R<Void> update(@PathVariable String permissionCode,
                          @RequestBody ChannelPermissionUpdateDTO dto) {
        channelPermissionService.update(permissionCode, dto);
        return R.ok();
    }

    @Operation(summary = "删除渠道权限")
    @OperationLog(module = "渠道权限", action = "删除")
    @SaCheckPermission("channel:permission:delete")
    @DeleteMapping("/{permissionCode}")
    public R<Void> delete(@PathVariable String permissionCode) {
        channelPermissionService.delete(permissionCode);
        return R.ok();
    }
}
