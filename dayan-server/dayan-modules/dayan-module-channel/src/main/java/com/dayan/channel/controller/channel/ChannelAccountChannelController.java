package com.dayan.channel.controller.channel;

import com.dayan.channel.dto.ChannelAccountCreateDTO;
import com.dayan.channel.dto.ChannelAccountQueryDTO;
import com.dayan.channel.dto.ChannelAccountUpdateDTO;
import com.dayan.channel.service.ChannelAccountService;
import com.dayan.channel.service.ChannelInfoService;
import com.dayan.channel.vo.ChannelAccountVO;
import com.dayan.common.core.resp.PageResult;
import com.dayan.common.core.resp.R;
import com.dayan.common.log.operation.OperationLog;
import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * Channel 渠道端账号管理接口。
 *
 * <p>路径：{@code /channel-api/channel-accounts}。
 *
 * <p>防越权：所有操作经 {@code requireDescendant} 校验目标 channelCode 是当前渠道后代。
 */
@Tag(name = "Channel 账号管理")
@RestController
@RequestMapping("/channel-accounts")
@RequiredArgsConstructor
public class ChannelAccountChannelController {

    private final ChannelAccountService channelAccountService;
    private final ChannelInfoService channelInfoService;

    @Operation(summary = "账号分页列表")
    @SaCheckPermission("channel:account:list")
    @GetMapping
    public R<PageResult<ChannelAccountVO>> page(ChannelAccountQueryDTO query) {
        if (query.getChannelCode() != null && !query.getChannelCode().isEmpty()) {
            channelInfoService.requireDescendant(query.getChannelCode());
        }
        return R.ok(channelAccountService.page(query));
    }

    @Operation(summary = "账号详情")
    @SaCheckPermission("channel:account:query")
    @GetMapping("/{accountCode}")
    public R<ChannelAccountVO> getDetail(@PathVariable String accountCode) {
        ChannelAccountVO vo = channelAccountService.getDetail(accountCode);
        if (vo != null && vo.getChannelCode() != null) {
            channelInfoService.requireDescendant(vo.getChannelCode());
        }
        return R.ok(vo);
    }

    @Operation(summary = "新建账号")
    @OperationLog(module = "渠道账号", action = "新增")
    @SaCheckPermission("channel:account:create")
    @PostMapping
    public R<String> create(@RequestBody @Valid ChannelAccountCreateDTO dto) {
        channelInfoService.requireDescendant(dto.getChannelCode());
        return R.ok(channelAccountService.create(dto));
    }

    @Operation(summary = "编辑账号")
    @OperationLog(module = "渠道账号", action = "修改")
    @SaCheckPermission("channel:account:update")
    @PutMapping("/{accountCode}")
    public R<Void> update(@PathVariable String accountCode,
                          @RequestBody ChannelAccountUpdateDTO dto) {
        ChannelAccountVO existing = channelAccountService.getDetail(accountCode);
        if (existing != null && existing.getChannelCode() != null) {
            channelInfoService.requireDescendant(existing.getChannelCode());
        }
        channelAccountService.update(accountCode, dto);
        return R.ok();
    }

    @Operation(summary = "重置密码")
    @OperationLog(module = "渠道账号", action = "重置密码")
    @SaCheckPermission("channel:account:reset")
    @PutMapping("/{accountCode}/reset-password")
    public R<Void> resetPassword(@PathVariable String accountCode) {
        ChannelAccountVO existing = channelAccountService.getDetail(accountCode);
        if (existing != null && existing.getChannelCode() != null) {
            channelInfoService.requireDescendant(existing.getChannelCode());
        }
        channelAccountService.resetPassword(accountCode);
        return R.ok();
    }

    @Operation(summary = "删除账号")
    @OperationLog(module = "渠道账号", action = "删除")
    @SaCheckPermission("channel:account:delete")
    @DeleteMapping("/{accountCode}")
    public R<Void> delete(@PathVariable String accountCode) {
        ChannelAccountVO existing = channelAccountService.getDetail(accountCode);
        if (existing != null && existing.getChannelCode() != null) {
            channelInfoService.requireDescendant(existing.getChannelCode());
        }
        channelAccountService.delete(accountCode);
        return R.ok();
    }
}
