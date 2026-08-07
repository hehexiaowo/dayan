package com.dayan.channel.controller.channel;

import com.dayan.channel.dto.ChannelInfoCreateDTO;
import com.dayan.channel.dto.ChannelInfoUpdateDTO;
import com.dayan.channel.service.ChannelInfoService;
import com.dayan.common.core.resp.R;
import com.dayan.common.log.operation.OperationLog;
import com.dayan.common.mybatis.context.ContextHolder;
import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * Channel 渠道端架构管理接口（子渠道建/改/删）。
 *
 * <p>路径：{@code /channel-api/channel-infos}（与 ChannelSelfController 共存，后者提供 GET /current、/tree）。
 *
 * <p>防越权：建/改/删均校验 {@code requireManageCapability()}（当前渠道 can_manage==1）
 * + {@code requireDescendant()}（目标渠道是当前渠道后代）。can_manage 不可自改（UpdateDTO 不含此字段）。
 */
@Tag(name = "Channel 架构管理")
@RestController
@RequestMapping("/channel-infos")
@RequiredArgsConstructor
public class ChannelInfoChannelController {

    private final ChannelInfoService channelInfoService;

    @Operation(summary = "新建子渠道")
    @OperationLog(module = "渠道架构", action = "新建")
    @SaCheckPermission("channel:info:create")
    @PostMapping
    public R<String> create(@RequestBody @Valid ChannelInfoCreateDTO dto) {
        // 管理能力校验 + parentCode 必须是当前渠道或其后代
        channelInfoService.requireManageCapability();
        if (dto.getParentCode() == null || dto.getParentCode().isEmpty()) {
            dto.setParentCode(ContextHolder.getChannelCode());
        }
        channelInfoService.requireDescendant(dto.getParentCode());
        return R.ok(channelInfoService.create(dto));
    }

    @Operation(summary = "编辑渠道信息")
    @OperationLog(module = "渠道架构", action = "修改")
    @SaCheckPermission("channel:info:update")
    @PutMapping("/{channelCode}")
    public R<Void> update(@PathVariable String channelCode,
                          @RequestBody ChannelInfoUpdateDTO dto) {
        channelInfoService.requireManageCapability();
        channelInfoService.requireDescendant(channelCode);
        channelInfoService.update(channelCode, dto);
        return R.ok();
    }

    @Operation(summary = "删除子渠道")
    @OperationLog(module = "渠道架构", action = "删除")
    @SaCheckPermission("channel:info:delete")
    @DeleteMapping("/{channelCode}")
    public R<Void> delete(@PathVariable String channelCode) {
        channelInfoService.requireManageCapability();
        channelInfoService.requireDescendant(channelCode);
        channelInfoService.delete(channelCode);
        return R.ok();
    }
}
