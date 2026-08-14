package com.dayan.organ.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.dayan.common.core.resp.PageResult;
import com.dayan.common.core.resp.R;
import com.dayan.organ.entity.OrganAccount;
import com.dayan.organ.service.OrganAccountService;
import com.dayan.organ.vo.OrganAccountVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * Admin 端账号管理接口。
 */
@Tag(name = "账号管理")
@RestController
@RequestMapping("/accounts")
@RequiredArgsConstructor
public class OrganAccountAdminController {

    private final OrganAccountService accountService;

    @Operation(summary = "账号分页列表")
    @SaCheckPermission("organ:account:list")
    @GetMapping
    public R<PageResult<OrganAccountVO>> page(
            @RequestParam(required = false) String organCode,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String realName,
            @RequestParam(required = false) Integer accountStatus,
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "20") long size) {
        return R.ok(accountService.page(organCode, username, realName, accountStatus, current, size));
    }

    @Operation(summary = "账号详情")
    @SaCheckPermission("organ:account:query")
    @GetMapping("/{accountCode}")
    public R<OrganAccountVO> getDetail(@PathVariable String accountCode) {
        return R.ok(accountService.getDetail(accountCode));
    }

    @Operation(summary = "新增账号")
    @SaCheckPermission("organ:account:create")
    @PostMapping
    public R<String> create(@RequestBody OrganAccount account) {
        return R.ok(accountService.create(account));
    }

    @Operation(summary = "修改账号")
    @SaCheckPermission("organ:account:update")
    @PutMapping("/{accountCode}")
    public R<Void> update(@PathVariable String accountCode, @RequestBody OrganAccount account) {
        accountService.update(accountCode, account);
        return R.ok();
    }

    @Operation(summary = "重置密码")
    @SaCheckPermission("organ:account:reset")
    @PutMapping("/{accountCode}/reset-password")
    public R<Void> resetPassword(@PathVariable String accountCode) {
        accountService.resetPassword(accountCode);
        return R.ok();
    }

    @Operation(summary = "切换账号状态")
    @SaCheckPermission("organ:account:status")
    @PutMapping("/{accountCode}/status/{status}")
    public R<Void> switchStatus(@PathVariable String accountCode, @PathVariable Integer status) {
        accountService.switchStatus(accountCode, status);
        return R.ok();
    }

    @Operation(summary = "删除账号")
    @SaCheckPermission("organ:account:delete")
    @DeleteMapping("/{accountCode}")
    public R<Void> delete(@PathVariable String accountCode) {
        accountService.delete(accountCode);
        return R.ok();
    }
}
