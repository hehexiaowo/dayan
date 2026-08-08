package com.dayan.channel.controller.channel;

import com.dayan.channel.entity.ChannelConfigContent;
import com.dayan.channel.entity.ChannelConfigScene;
import com.dayan.channel.service.ChannelConfigContentService;
import com.dayan.channel.service.ChannelConfigSceneService;
import com.dayan.common.core.resp.R;
import com.dayan.common.mybatis.context.ContextHolder;
import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Channel 渠道端配置自管接口（内容/场景展示配置）。
 *
 * <p>路径：{@code /channel-api/channel-configs}。
 * 防越权：channelCode 从 {@link ContextHolder} 强制注入，忽略前端传入。
 * 渠道自己管理其下游（agent/client）展示哪些平台内容/场景。
 */
@Tag(name = "Channel 配置自管")
@RestController
@RequestMapping("/channel-configs")
@RequiredArgsConstructor
public class ChannelConfigSelfController {

    private final ChannelConfigContentService channelConfigContentService;
    private final ChannelConfigSceneService channelConfigSceneService;

    // ====== 内容配置（appType: agent/client）======

    @Operation(summary = "查本渠道内容配置")
    @SaCheckPermission("channel:contentConfig:list")
    @GetMapping("/content")
    public R<List<ChannelConfigContent>> listContent(
            @Parameter(description = "应用类型：agent/client/null(全部)") @RequestParam(required = false) String appType) {
        String channelCode = ContextHolder.getChannelCode();
        List<ChannelConfigContent> all = channelConfigContentService.listByChannel(channelCode);
        if (appType != null && !appType.isEmpty()) {
            return R.ok(all.stream().filter(c -> appType.equals(c.getAppType())).collect(Collectors.toList()));
        }
        return R.ok(all);
    }

    @Operation(summary = "保存本渠道内容配置（全量覆盖）")
    @SaCheckPermission("channel:contentConfig:save")
    @PutMapping("/content")
    public R<Void> saveContent(@RequestBody List<ChannelConfigContent> configs) {
        String channelCode = ContextHolder.getChannelCode();
        // 强制注入 channelCode，防越权（忽略前端传入）
        configs.forEach(c -> c.setChannelCode(channelCode));
        channelConfigContentService.saveAll(channelCode, configs);
        return R.ok();
    }

    // ====== 场景配置 ======

    @Operation(summary = "查本渠道场景配置")
    @SaCheckPermission("channel:contentConfig:list")
    @GetMapping("/scene")
    public R<List<ChannelConfigScene>> listScene() {
        return R.ok(channelConfigSceneService.listByChannel(ContextHolder.getChannelCode()));
    }

    @Operation(summary = "保存本渠道场景配置（全量覆盖）")
    @SaCheckPermission("channel:contentConfig:save")
    @PutMapping("/scene")
    public R<Void> saveScene(@RequestBody List<ChannelConfigScene> configs) {
        String channelCode = ContextHolder.getChannelCode();
        configs.forEach(c -> c.setChannelCode(channelCode));
        channelConfigSceneService.saveAll(channelCode, configs);
        return R.ok();
    }
}
