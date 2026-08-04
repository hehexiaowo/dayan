package com.dayan.channel.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.dayan.channel.dto.ChannelOpenPlatformCreateDTO;
import com.dayan.channel.dto.ChannelOpenPlatformQueryDTO;
import com.dayan.channel.dto.ChannelOpenPlatformUpdateDTO;
import com.dayan.channel.service.ChannelOpenPlatformService;
import com.dayan.channel.vo.ChannelOpenPlatformVO;
import com.dayan.common.core.resp.PageResult;
import com.dayan.common.core.resp.R;
import com.dayan.common.log.operation.OperationLog;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * Admin 端渠道开放平台配置接口。
 *
 * <p>路径：{@code /open-platforms/*}。{@code appSecret} 入参明文、出参脱敏。
 */
@Tag(name = "渠道开放平台配置")
@RestController
@RequestMapping("/open-platforms")
@RequiredArgsConstructor
public class ChannelOpenPlatformAdminController {

    private final ChannelOpenPlatformService channelOpenPlatformService;

    @Operation(summary = "开放平台配置分页列表")
    @SaCheckPermission("channel:openplatform:list")
    @GetMapping
    public R<PageResult<ChannelOpenPlatformVO>> page(ChannelOpenPlatformQueryDTO query) {
        return R.ok(channelOpenPlatformService.page(query));
    }

    @Operation(summary = "开放平台配置详情")
    @SaCheckPermission("channel:openplatform:query")
    @GetMapping("/{id}")
    public R<ChannelOpenPlatformVO> getDetail(@PathVariable Long id) {
        return R.ok(channelOpenPlatformService.getDetail(id));
    }

    @Operation(summary = "新增开放平台配置")
    @OperationLog(module = "开放平台配置", action = "新增")
    @SaCheckPermission("channel:openplatform:create")
    @PostMapping
    public R<Long> create(@RequestBody @Valid ChannelOpenPlatformCreateDTO dto) {
        return R.ok(channelOpenPlatformService.create(dto));
    }

    @Operation(summary = "修改开放平台配置")
    @OperationLog(module = "开放平台配置", action = "修改")
    @SaCheckPermission("channel:openplatform:update")
    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id,
                          @RequestBody ChannelOpenPlatformUpdateDTO dto) {
        channelOpenPlatformService.update(id, dto);
        return R.ok();
    }

    @Operation(summary = "删除开放平台配置")
    @OperationLog(module = "开放平台配置", action = "删除")
    @SaCheckPermission("channel:openplatform:delete")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        channelOpenPlatformService.delete(id);
        return R.ok();
    }
}
