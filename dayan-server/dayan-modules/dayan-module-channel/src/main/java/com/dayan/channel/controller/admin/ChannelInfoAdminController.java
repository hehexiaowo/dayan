package com.dayan.channel.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.dayan.channel.dto.ChannelAuditDTO;
import com.dayan.channel.dto.ChannelInfoCreateDTO;
import com.dayan.channel.dto.ChannelInfoQueryDTO;
import com.dayan.channel.dto.ChannelInfoUpdateDTO;
import com.dayan.channel.service.ChannelInfoService;
import com.dayan.channel.vo.ChannelInfoVO;
import com.dayan.common.core.resp.R;
import com.dayan.common.log.operation.OperationLog;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Admin 端渠道信息接口（树形）。
 *
 * <p>路径：{@code /channels/*}（由 dayan-admin 启动模块 context-path=/admin-api 拼接为
 * {@code /admin-api/channels/*}）。
 */
@Tag(name = "渠道管理")
@RestController
@RequestMapping("/channels")
@RequiredArgsConstructor
public class ChannelInfoAdminController {

    private final ChannelInfoService channelInfoService;

    @Operation(summary = "渠道列表（按父渠道查询子级，平铺）")
    @SaCheckPermission("channel:info:list")
    @GetMapping
    public R<List<ChannelInfoVO>> list(ChannelInfoQueryDTO query) {
        return R.ok(channelInfoService.listByParent(query));
    }

    @Operation(summary = "渠道树")
    @SaCheckPermission("channel:info:list")
    @GetMapping("/tree")
    public R<List<ChannelInfoVO>> tree() {
        return R.ok(channelInfoService.tree());
    }

    @Operation(summary = "渠道详情")
    @SaCheckPermission("channel:info:query")
    @GetMapping("/{channelCode}")
    public R<ChannelInfoVO> getDetail(@PathVariable String channelCode) {
        return R.ok(channelInfoService.getDetail(channelCode));
    }

    @Operation(summary = "新增渠道")
    @OperationLog(module = "渠道管理", action = "新增")
    @SaCheckPermission("channel:info:create")
    @PostMapping
    public R<String> create(@RequestBody @Valid ChannelInfoCreateDTO dto) {
        return R.ok(channelInfoService.create(dto));
    }

    @Operation(summary = "修改渠道")
    @OperationLog(module = "渠道管理", action = "修改")
    @SaCheckPermission("channel:info:update")
    @PutMapping("/{channelCode}")
    public R<Void> update(@PathVariable String channelCode,
                          @RequestBody ChannelInfoUpdateDTO dto) {
        channelInfoService.update(channelCode, dto);
        return R.ok();
    }

    @Operation(summary = "删除渠道")
    @OperationLog(module = "渠道管理", action = "删除")
    @SaCheckPermission("channel:info:delete")
    @DeleteMapping("/{channelCode}")
    public R<Void> delete(@PathVariable String channelCode) {
        channelInfoService.delete(channelCode);
        return R.ok();
    }

    @Operation(summary = "审核渠道（待审→通过/驳回）")
    @OperationLog(module = "渠道管理", action = "审核")
    @SaCheckPermission("channel:info:audit")
    @PostMapping("/audit")
    public R<Void> audit(@RequestBody @Valid ChannelAuditDTO dto) {
        channelInfoService.audit(dto);
        return R.ok();
    }
}
