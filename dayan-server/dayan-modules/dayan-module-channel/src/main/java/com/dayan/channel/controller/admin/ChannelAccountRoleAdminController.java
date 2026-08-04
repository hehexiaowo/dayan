package com.dayan.channel.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.dayan.channel.service.ChannelAccountRoleService;
import com.dayan.common.core.resp.R;
import com.dayan.common.log.operation.OperationLog;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Admin 端渠道账号-角色关联接口。
 *
 * <p>路径：{@code /channel-account-roles/*}。
 */
@Tag(name = "渠道账号角色")
@RestController
@RequestMapping("/channel-account-roles")
@RequiredArgsConstructor
public class ChannelAccountRoleAdminController {

    private final ChannelAccountRoleService channelAccountRoleService;

    @Operation(summary = "给渠道账号分配角色（全量覆盖）")
    @OperationLog(module = "渠道账号角色", action = "分配")
    @SaCheckPermission("channel:account:assign")
    @PutMapping("/{accountCode}/roles")
    public R<Void> assignRoles(@PathVariable String accountCode,
                               @RequestBody List<String> roleCodes) {
        channelAccountRoleService.assignRoles(accountCode, roleCodes);
        return R.ok();
    }

    @Operation(summary = "查询渠道账号的角色编码列表")
    @SaCheckPermission("channel:account:assign")
    @GetMapping("/{accountCode}/roles")
    public R<List<String>> listRoles(@PathVariable String accountCode) {
        return R.ok(channelAccountRoleService.listRoles(accountCode));
    }
}
