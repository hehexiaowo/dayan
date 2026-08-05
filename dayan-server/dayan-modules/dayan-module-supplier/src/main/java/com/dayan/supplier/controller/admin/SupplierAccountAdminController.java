package com.dayan.supplier.controller.admin;

import com.dayan.common.core.resp.PageResult;
import com.dayan.common.core.resp.R;
import com.dayan.common.log.operation.OperationLog;
import com.dayan.supplier.dto.SupplierAccountCreateDTO;
import com.dayan.supplier.dto.SupplierAccountQueryDTO;
import com.dayan.supplier.dto.SupplierAccountUpdateDTO;
import com.dayan.supplier.service.SupplierAccountRoleService;
import com.dayan.supplier.service.SupplierAccountService;
import com.dayan.supplier.vo.SupplierAccountVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Admin 端供应商账号管理接口。
 *
 * <p>路径：{@code /supplier/account/*}（由 dayan-admin context-path=/admin-api 拼接为
 * {@code /admin-api/supplier/account/*}）。
 */
@Tag(name = "供应商账号管理")
@RestController
@RequestMapping("/supplier/account")
@RequiredArgsConstructor
public class SupplierAccountAdminController {

    private final SupplierAccountService supplierAccountService;
    private final SupplierAccountRoleService supplierAccountRoleService;

    @Operation(summary = "供应商账号分页列表")
    @SaCheckPermission("supplier:account:list")
    @GetMapping("/page")
    public R<PageResult<SupplierAccountVO>> page(SupplierAccountQueryDTO query) {
        return R.ok(supplierAccountService.page(query));
    }

    @Operation(summary = "供应商账号详情")
    @SaCheckPermission("supplier:account:query")
    @GetMapping("/{accountCode}")
    public R<SupplierAccountVO> getDetail(@PathVariable String accountCode) {
        return R.ok(supplierAccountService.getDetail(accountCode));
    }

    @Operation(summary = "新增供应商账号")
    @OperationLog(module = "供应商账号", action = "新增")
    @SaCheckPermission("supplier:account:create")
    @PostMapping
    public R<String> create(@RequestBody @Valid SupplierAccountCreateDTO dto) {
        return R.ok(supplierAccountService.create(dto));
    }

    @Operation(summary = "修改供应商账号")
    @OperationLog(module = "供应商账号", action = "修改")
    @SaCheckPermission("supplier:account:update")
    @PutMapping
    public R<Void> update(@RequestParam String accountCode,
                          @RequestBody SupplierAccountUpdateDTO dto) {
        supplierAccountService.update(accountCode, dto);
        return R.ok();
    }

    @Operation(summary = "重置供应商账号密码")
    @OperationLog(module = "供应商账号", action = "重置密码")
    @SaCheckPermission("supplier:account:reset")
    @PutMapping("/{accountCode}/reset-password")
    public R<Void> resetPassword(@PathVariable String accountCode) {
        supplierAccountService.resetPassword(accountCode);
        return R.ok();
    }

    @Operation(summary = "删除供应商账号")
    @OperationLog(module = "供应商账号", action = "删除")
    @SaCheckPermission("supplier:account:delete")
    @DeleteMapping("/{accountCode}")
    public R<Void> delete(@PathVariable String accountCode) {
        supplierAccountService.delete(accountCode);
        return R.ok();
    }

    @Operation(summary = "给供应商账号分配角色（全量覆盖）")
    @OperationLog(module = "供应商账号", action = "分配角色")
    @SaCheckPermission("supplier:account:assign-roles")
    @PutMapping("/{accountCode}/roles")
    public R<Void> assignRoles(@PathVariable String accountCode,
                               @RequestBody List<String> roleCodes) {
        supplierAccountRoleService.assignRoles(accountCode, roleCodes);
        return R.ok();
    }

    @Operation(summary = "查询供应商账号的角色编码列表")
    @SaCheckPermission("supplier:account:query-roles")
    @GetMapping("/{accountCode}/roles")
    public R<List<String>> listRoles(@PathVariable String accountCode) {
        return R.ok(supplierAccountRoleService.listRoles(accountCode));
    }
}
