package com.dayan.channel.controller.channel;

import com.dayan.channel.service.ChannelAccountRoleService;
import com.dayan.channel.service.ChannelAccountService;
import com.dayan.channel.service.ChannelInfoService;
import com.dayan.channel.vo.ChannelAccountVO;
import com.dayan.common.core.resp.R;
import com.dayan.common.log.operation.OperationLog;
import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Channel 渠道端账号-角色分配接口。
 *
 * <p>路径：{@code /channel-api/channel-account-roles}。
 */
@Tag(name = "Channel 账号角色分配")
@RestController
@RequestMapping("/channel-account-roles")
@RequiredArgsConstructor
public class ChannelAccountRoleChannelController {

    private final ChannelAccountRoleService channelAccountRoleService;
    private final ChannelAccountService channelAccountService;
    private final ChannelInfoService channelInfoService;

    @Operation(summary = "分配角色（先删后增）")
    @OperationLog(module = "账号角色", action = "分配")
    @SaCheckPermission("channel:account:assign")
    @PutMapping("/{accountCode}/roles")
    public R<Void> assignRoles(@PathVariable String accountCode,
                               @RequestBody List<String> roleCodes) {
        requireAccountInSubtree(accountCode);
        channelAccountRoleService.assignRoles(accountCode, roleCodes);
        return R.ok();
    }

    @Operation(summary = "查询账号已分配角色码")
    @SaCheckPermission("channel:account:query")
    @GetMapping("/{accountCode}/roles")
    public R<List<String>> listRoles(@PathVariable String accountCode) {
        requireAccountInSubtree(accountCode);
        return R.ok(channelAccountRoleService.listRoles(accountCode));
    }

    /** 校验账号归属本渠道子树 */
    private void requireAccountInSubtree(String accountCode) {
        ChannelAccountVO vo = channelAccountService.getDetail(accountCode);
        if (vo != null && vo.getChannelCode() != null) {
            channelInfoService.requireDescendant(vo.getChannelCode());
        }
    }
}
