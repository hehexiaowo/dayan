package com.dayan.channel.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.dayan.channel.entity.ChannelConfigContent;
import com.dayan.channel.entity.ChannelConfigCourse;
import com.dayan.channel.entity.ChannelConfigGoods;
import com.dayan.channel.entity.ChannelConfigScene;
import com.dayan.channel.entity.ChannelConfigTool;
import com.dayan.channel.service.ChannelConfigContentService;
import com.dayan.channel.service.ChannelConfigCourseService;
import com.dayan.channel.service.ChannelConfigGoodsService;
import com.dayan.channel.service.ChannelConfigSceneService;
import com.dayan.channel.service.ChannelConfigToolService;
import com.dayan.common.core.resp.R;
import com.dayan.common.log.operation.OperationLog;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Admin 端渠道五类配置（内容/场景/商品/课程/工具）统一接口。
 *
 * <p>路径：{@code /channel-configs/*}，通过 {@code type} 路径参数区分：
 * <ul>
 *   <li>{@code content} - 内容配置（ChannelConfigContent）</li>
 *   <li>{@code scene} - 场景配置（ChannelConfigScene）</li>
 *   <li>{@code goods} - 商品配置（ChannelConfigGoods）</li>
 *   <li>{@code course} - 课程配置（ChannelConfigCourse）</li>
 *   <li>{@code tool} - 工具配置（ChannelConfigTool）</li>
 * </ul>
 *
 * <p>每类均提供：按渠道查询 + 批量保存（全量覆盖）两个操作。
 */
@Tag(name = "渠道配置管理")
@RestController
@RequestMapping("/channel-configs")
@RequiredArgsConstructor
public class ChannelConfigAdminController {

    private final ChannelConfigContentService configContentService;
    private final ChannelConfigSceneService configSceneService;
    private final ChannelConfigGoodsService configGoodsService;
    private final ChannelConfigCourseService configCourseService;
    private final ChannelConfigToolService configToolService;

    // ====== 内容配置 ======

    @Operation(summary = "查询渠道内容配置")
    @SaCheckPermission("channel:config:query")
    @GetMapping("/{channelCode}/content")
    public R<List<ChannelConfigContent>> listContent(@PathVariable String channelCode) {
        return R.ok(configContentService.listByChannel(channelCode));
    }

    @Operation(summary = "保存渠道内容配置（全量覆盖）")
    @OperationLog(module = "渠道内容配置", action = "保存")
    @SaCheckPermission("channel:config:save")
    @PutMapping("/{channelCode}/content")
    public R<Void> saveContent(@PathVariable String channelCode,
                               @RequestBody List<ChannelConfigContent> configs) {
        configContentService.saveAll(channelCode, configs);
        return R.ok();
    }

    // ====== 场景配置 ======

    @Operation(summary = "查询渠道场景配置")
    @SaCheckPermission("channel:config:query")
    @GetMapping("/{channelCode}/scene")
    public R<List<ChannelConfigScene>> listScene(@PathVariable String channelCode) {
        return R.ok(configSceneService.listByChannel(channelCode));
    }

    @Operation(summary = "保存渠道场景配置（全量覆盖）")
    @OperationLog(module = "渠道场景配置", action = "保存")
    @SaCheckPermission("channel:config:save")
    @PutMapping("/{channelCode}/scene")
    public R<Void> saveScene(@PathVariable String channelCode,
                             @RequestBody List<ChannelConfigScene> configs) {
        configSceneService.saveAll(channelCode, configs);
        return R.ok();
    }

    // ====== 商品配置 ======

    @Operation(summary = "查询渠道商品配置")
    @SaCheckPermission("channel:config:query")
    @GetMapping("/{channelCode}/goods")
    public R<List<ChannelConfigGoods>> listGoods(@PathVariable String channelCode) {
        return R.ok(configGoodsService.listByChannel(channelCode));
    }

    @Operation(summary = "保存渠道商品配置（全量覆盖）")
    @OperationLog(module = "渠道商品配置", action = "保存")
    @SaCheckPermission("channel:config:save")
    @PutMapping("/{channelCode}/goods")
    public R<Void> saveGoods(@PathVariable String channelCode,
                             @RequestBody List<ChannelConfigGoods> configs) {
        configGoodsService.saveAll(channelCode, configs);
        return R.ok();
    }

    // ====== 课程配置 ======

    @Operation(summary = "查询渠道课程配置")
    @SaCheckPermission("channel:config:query")
    @GetMapping("/{channelCode}/course")
    public R<List<ChannelConfigCourse>> listCourse(@PathVariable String channelCode) {
        return R.ok(configCourseService.listByChannel(channelCode));
    }

    @Operation(summary = "保存渠道课程配置（全量覆盖）")
    @OperationLog(module = "渠道课程配置", action = "保存")
    @SaCheckPermission("channel:config:save")
    @PutMapping("/{channelCode}/course")
    public R<Void> saveCourse(@PathVariable String channelCode,
                              @RequestBody List<ChannelConfigCourse> configs) {
        configCourseService.saveAll(channelCode, configs);
        return R.ok();
    }

    // ====== 工具配置 ======

    @Operation(summary = "查询渠道工具配置")
    @SaCheckPermission("channel:config:query")
    @GetMapping("/{channelCode}/tool")
    public R<List<ChannelConfigTool>> listTool(@PathVariable String channelCode) {
        return R.ok(configToolService.listByChannel(channelCode));
    }

    @Operation(summary = "保存渠道工具配置（全量覆盖）")
    @OperationLog(module = "渠道工具配置", action = "保存")
    @SaCheckPermission("channel:config:save")
    @PutMapping("/{channelCode}/tool")
    public R<Void> saveTool(@PathVariable String channelCode,
                            @RequestBody List<ChannelConfigTool> configs) {
        configToolService.saveAll(channelCode, configs);
        return R.ok();
    }
}
