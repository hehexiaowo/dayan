package com.dayan.channel.controller.channel;

import com.dayan.channel.dto.ChannelRoleCreateDTO;
import com.dayan.channel.dto.ChannelRoleQueryDTO;
import com.dayan.channel.dto.ChannelRoleUpdateDTO;
import com.dayan.channel.entity.ChannelRole;
import com.dayan.channel.service.ChannelRoleService;
import com.dayan.common.core.exception.BusinessException;
import com.dayan.common.core.exception.ErrorCode;
import com.dayan.common.core.resp.PageResult;
import com.dayan.common.core.resp.R;
import com.dayan.common.log.operation.OperationLog;
import com.dayan.common.mybatis.context.ContextHolder;
import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Channel 渠道端角色管理接口。
 *
 * <p>路径：{@code /channel-api/channel-roles}。
 *
 * <p>防越权：角色限本渠道（channelCode = 当前渠道），用 eq 校验（不跨渠道）。
 * create 时强制覆盖 channelCode 为当前渠道，防止前端伪造。
 */
@Tag(name = "Channel 角色管理")
@RestController
@RequestMapping("/channel-roles")
@RequiredArgsConstructor
public class ChannelRoleChannelController {

    private final ChannelRoleService channelRoleService;

    @Operation(summary = "角色分页列表")
    @SaCheckPermission("channel:role:list")
    @GetMapping
    public R<PageResult<ChannelRole>> page(ChannelRoleQueryDTO query) {
        query.setChannelCode(ContextHolder.getChannelCode());
        return R.ok(channelRoleService.page(query));
    }

    @Operation(summary = "角色详情")
    @SaCheckPermission("channel:role:query")
    @GetMapping("/{roleCode}")
    public R<ChannelRole> getDetail(@PathVariable String roleCode) {
        ChannelRole role = channelRoleService.getDetail(roleCode);
        requireOwnChannel(role);
        return R.ok(role);
    }

    @Operation(summary = "新建角色")
    @OperationLog(module = "渠道角色", action = "新增")
    @SaCheckPermission("channel:role:create")
    @PostMapping
    public R<String> create(@RequestBody @Valid ChannelRoleCreateDTO dto) {
        dto.setChannelCode(ContextHolder.getChannelCode()); // 强制覆盖，防伪造
        return R.ok(channelRoleService.create(dto));
    }

    @Operation(summary = "编辑角色")
    @OperationLog(module = "渠道角色", action = "修改")
    @SaCheckPermission("channel:role:update")
    @PutMapping("/{roleCode}")
    public R<Void> update(@PathVariable String roleCode,
                          @RequestBody ChannelRoleUpdateDTO dto) {
        requireOwnChannel(channelRoleService.getDetail(roleCode));
        channelRoleService.update(roleCode, dto);
        return R.ok();
    }

    @Operation(summary = "删除角色")
    @OperationLog(module = "渠道角色", action = "删除")
    @SaCheckPermission("channel:role:delete")
    @DeleteMapping("/{roleCode}")
    public R<Void> delete(@PathVariable String roleCode) {
        requireOwnChannel(channelRoleService.getDetail(roleCode));
        channelRoleService.delete(roleCode);
        return R.ok();
    }

    @Operation(summary = "角色授权（权限码列表，先删后增）")
    @OperationLog(module = "渠道角色", action = "授权")
    @SaCheckPermission("channel:role:assign")
    @PutMapping("/{roleCode}/permissions")
    public R<Void> assignPermissions(@PathVariable String roleCode,
                                     @RequestBody List<String> permissionCodes) {
        requireOwnChannel(channelRoleService.getDetail(roleCode));
        channelRoleService.assignPermissions(roleCode, permissionCodes);
        return R.ok();
    }

    @Operation(summary = "查询角色已分配权限码")
    @SaCheckPermission("channel:role:query")
    @GetMapping("/{roleCode}/permissions")
    public R<List<String>> listPermissions(@PathVariable String roleCode) {
        requireOwnChannel(channelRoleService.getDetail(roleCode));
        return R.ok(channelRoleService.listPermissions(roleCode));
    }

    /** 校验角色归属当前渠道 */
    private void requireOwnChannel(ChannelRole role) {
        if (role == null || !ContextHolder.getChannelCode().equals(role.getChannelCode())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权操作非本渠道的角色");
        }
    }
}
