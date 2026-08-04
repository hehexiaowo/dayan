package com.dayan.channel.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.dayan.channel.dto.ChannelRoleCreateDTO;
import com.dayan.channel.dto.ChannelRoleQueryDTO;
import com.dayan.channel.dto.ChannelRoleUpdateDTO;
import com.dayan.channel.entity.ChannelRole;
import com.dayan.channel.service.ChannelRoleService;
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
 * Admin 端渠道角色管理接口（P2 简化：基础 CRUD + 授权）。
 *
 * <p>路径：{@code /channel-roles/*}。RBAC 鉴权回调（ChannelStpInterface）后置实现。
 */
@Tag(name = "渠道角色管理")
@RestController
@RequestMapping("/channel-roles")
@RequiredArgsConstructor
public class ChannelRoleAdminController {

    private final ChannelRoleService channelRoleService;

    @Operation(summary = "渠道角色分页列表")
    @SaCheckPermission("channel:role:list")
    @GetMapping
    public R<PageResult<ChannelRole>> page(ChannelRoleQueryDTO query) {
        return R.ok(channelRoleService.page(query));
    }

    @Operation(summary = "渠道角色详情")
    @SaCheckPermission("channel:role:query")
    @GetMapping("/{roleCode}")
    public R<ChannelRole> getDetail(@PathVariable String roleCode) {
        return R.ok(channelRoleService.getDetail(roleCode));
    }

    @Operation(summary = "新增渠道角色")
    @OperationLog(module = "渠道角色", action = "新增")
    @SaCheckPermission("channel:role:create")
    @PostMapping
    public R<String> create(@RequestBody @Valid ChannelRoleCreateDTO dto) {
        return R.ok(channelRoleService.create(dto));
    }

    @Operation(summary = "修改渠道角色")
    @OperationLog(module = "渠道角色", action = "修改")
    @SaCheckPermission("channel:role:update")
    @PutMapping("/{roleCode}")
    public R<Void> update(@PathVariable String roleCode,
                          @RequestBody ChannelRoleUpdateDTO dto) {
        channelRoleService.update(roleCode, dto);
        return R.ok();
    }

    @Operation(summary = "删除渠道角色")
    @OperationLog(module = "渠道角色", action = "删除")
    @SaCheckPermission("channel:role:delete")
    @DeleteMapping("/{roleCode}")
    public R<Void> delete(@PathVariable String roleCode) {
        channelRoleService.delete(roleCode);
        return R.ok();
    }

    @Operation(summary = "给渠道角色授权（全量覆盖）")
    @OperationLog(module = "渠道角色", action = "授权")
    @SaCheckPermission("channel:role:assign")
    @PutMapping("/{roleCode}/permissions")
    public R<Void> assignPermissions(@PathVariable String roleCode,
                                     @RequestBody List<String> permissionCodes) {
        channelRoleService.assignPermissions(roleCode, permissionCodes);
        return R.ok();
    }

    @Operation(summary = "查询渠道角色权限码列表")
    @SaCheckPermission("channel:role:query")
    @GetMapping("/{roleCode}/permissions")
    public R<List<String>> listPermissions(@PathVariable String roleCode) {
        return R.ok(channelRoleService.listPermissions(roleCode));
    }
}
