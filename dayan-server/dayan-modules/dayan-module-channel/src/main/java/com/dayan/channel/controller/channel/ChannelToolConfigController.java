package com.dayan.channel.controller.channel;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.dayan.channel.service.ChannelConfigToolService;
import com.dayan.common.core.resp.R;
import com.dayan.common.mybatis.context.ContextHolder;
import com.dayan.tool.service.ToolInfoService;
import com.dayan.tool.vo.ToolInfoVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Channel 渠道端工具配置接口（工具可见性配置）。
 *
 * <p>路径 {@code /tools/config}（dayan-channel starter context-path 拼接为 {@code /channel-api/tools/config/*}）。
 *
 * <p>渠道隔离：channelCode 一律从 {@link ContextHolder} 强制注入，不接收前端参数。</p>
 */
@Tag(name = "Channel 工具配置")
@RestController
@RequestMapping("/tools/config")
@RequiredArgsConstructor
public class ChannelToolConfigController {

    private static final int CONFIG_TYPE_VISIBILITY = 0;

    private final ToolInfoService toolInfoService;
    private final ChannelConfigToolService channelConfigToolService;

    @Operation(summary = "可配置工具列表（全部启用工具）")
    @SaCheckPermission("channel:tool:config:view")
    @GetMapping("/available")
    public R<List<ToolInfoVO>> availableTools() {
        return R.ok(toolInfoService.listEnabled());
    }

    @Operation(summary = "本渠道已配置的工具编码列表")
    @SaCheckPermission("channel:tool:config:view")
    @GetMapping("/configured")
    public R<List<String>> configuredToolCodes() {
        String channelCode = ContextHolder.getChannelCode();
        return R.ok(channelConfigToolService.listConfiguredToolCodes(channelCode, CONFIG_TYPE_VISIBILITY));
    }

    @Operation(summary = "保存工具可见性配置（全量替换）")
    @SaCheckPermission("channel:tool:config:update")
    @PutMapping("/visibility")
    public R<Void> saveVisibility(@RequestBody List<String> toolCodes) {
        String channelCode = ContextHolder.getChannelCode();
        for (String toolCode : toolCodes) {
            channelConfigToolService.save(channelCode, toolCode, CONFIG_TYPE_VISIBILITY, "{}");
        }
        return R.ok();
    }
}
