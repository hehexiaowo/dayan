package com.dayan.client.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.dayan.client.dto.ClientAccountCreateDTO;
import com.dayan.client.dto.ClientAccountQueryDTO;
import com.dayan.client.dto.ClientAccountUpdateDTO;
import com.dayan.client.service.ClientAccountService;
import com.dayan.client.vo.ClientAccountVO;
import com.dayan.common.core.resp.PageResult;
import com.dayan.common.core.resp.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * Admin 端客户账号接口。
 */
@Tag(name = "客户账号管理")
@RestController
@RequestMapping("/client-accounts")
@RequiredArgsConstructor
public class ClientAccountAdminController {

    private final ClientAccountService clientAccountService;

    @Operation(summary = "客户账号分页列表（按渠道隔离）")
    @SaCheckPermission("client:account:list")
    @GetMapping
    public R<PageResult<ClientAccountVO>> page(ClientAccountQueryDTO query) {
        return R.ok(clientAccountService.page(query));
    }

    @Operation(summary = "新增客户账号")
    @SaCheckPermission("client:account:create")
    @PostMapping
    public R<String> create(@RequestBody @Valid ClientAccountCreateDTO dto) {
        return R.ok(clientAccountService.create(dto));
    }

    @Operation(summary = "修改客户账号")
    @SaCheckPermission("client:account:update")
    @PutMapping("/{clientCode}")
    public R<Void> update(@PathVariable String clientCode, @RequestBody @Valid ClientAccountUpdateDTO dto) {
        clientAccountService.update(clientCode, dto);
        return R.ok();
    }

    @Operation(summary = "重置密码")
    @SaCheckPermission("client:account:reset")
    @PutMapping("/{clientCode}/reset-password")
    public R<Void> resetPassword(@PathVariable String clientCode) {
        clientAccountService.resetPassword(clientCode);
        return R.ok();
    }

    @Operation(summary = "删除客户账号")
    @SaCheckPermission("client:account:delete")
    @DeleteMapping("/{clientCode}")
    public R<Void> delete(@PathVariable String clientCode) {
        clientAccountService.delete(clientCode);
        return R.ok();
    }
}
