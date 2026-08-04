package com.dayan.channel.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.dayan.channel.dto.ChannelAccountCreateDTO;
import com.dayan.channel.dto.ChannelAccountQueryDTO;
import com.dayan.channel.dto.ChannelAccountUpdateDTO;
import com.dayan.channel.service.ChannelAccountService;
import com.dayan.channel.vo.ChannelAccountVO;
import com.dayan.common.core.resp.PageResult;
import com.dayan.common.core.resp.R;
import com.dayan.common.log.operation.OperationLog;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * Admin 端渠道账号管理接口。
 *
 * <p>路径：{@code /channel-accounts/*}。
 */
@Tag(name = "渠道账号管理")
@RestController
@RequestMapping("/channel-accounts")
@RequiredArgsConstructor
public class ChannelAccountAdminController {

    private final ChannelAccountService channelAccountService;

    @Operation(summary = "渠道账号分页列表")
    @SaCheckPermission("channel:account:list")
    @GetMapping
    public R<PageResult<ChannelAccountVO>> page(ChannelAccountQueryDTO query) {
        return R.ok(channelAccountService.page(query));
    }

    @Operation(summary = "渠道账号详情")
    @SaCheckPermission("channel:account:query")
    @GetMapping("/{accountCode}")
    public R<ChannelAccountVO> getDetail(@PathVariable String accountCode) {
        return R.ok(channelAccountService.getDetail(accountCode));
    }

    @Operation(summary = "新增渠道账号")
    @OperationLog(module = "渠道账号", action = "新增")
    @SaCheckPermission("channel:account:create")
    @PostMapping
    public R<String> create(@RequestBody @Valid ChannelAccountCreateDTO dto) {
        return R.ok(channelAccountService.create(dto));
    }

    @Operation(summary = "修改渠道账号")
    @OperationLog(module = "渠道账号", action = "修改")
    @SaCheckPermission("channel:account:update")
    @PutMapping("/{accountCode}")
    public R<Void> update(@PathVariable String accountCode,
                          @RequestBody ChannelAccountUpdateDTO dto) {
        channelAccountService.update(accountCode, dto);
        return R.ok();
    }

    @Operation(summary = "重置密码")
    @OperationLog(module = "渠道账号", action = "重置密码")
    @SaCheckPermission("channel:account:reset")
    @PutMapping("/{accountCode}/reset-password")
    public R<Void> resetPassword(@PathVariable String accountCode) {
        channelAccountService.resetPassword(accountCode);
        return R.ok();
    }

    @Operation(summary = "删除渠道账号")
    @OperationLog(module = "渠道账号", action = "删除")
    @SaCheckPermission("channel:account:delete")
    @DeleteMapping("/{accountCode}")
    public R<Void> delete(@PathVariable String accountCode) {
        channelAccountService.delete(accountCode);
        return R.ok();
    }
}
