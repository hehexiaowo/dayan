package com.dayan.channel.controller.channel;

import com.dayan.channel.entity.ChannelPermission;
import com.dayan.channel.service.ChannelPermissionService;
import com.dayan.common.core.resp.R;
import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Channel 渠道端权限查询接口（只读，供角色授权弹窗选权限用）。
 *
 * <p>路径：{@code /channel-api/channel-permissions}。
 * <p>权限码全由后端 seed 定义，渠道端不做权限 CRUD（YAGNI）。
 */
@Tag(name = "Channel 权限查询")
@RestController
@RequestMapping("/channel-permissions")
@RequiredArgsConstructor
public class ChannelPermissionChannelController {

    private final ChannelPermissionService channelPermissionService;

    @Operation(summary = "全部启用权限（扁平列表）")
    @SaCheckPermission("channel:permission:list")
    @GetMapping("/all")
    public R<List<ChannelPermission>> all() {
        return R.ok(channelPermissionService.listAll());
    }

    @Operation(summary = "权限树（按 parentCode 组装）")
    @SaCheckPermission("channel:permission:list")
    @GetMapping("/tree")
    public R<List<ChannelPermission>> tree() {
        return R.ok(channelPermissionService.tree());
    }
}
