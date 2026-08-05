package com.dayan.supplier.controller.admin;

import com.dayan.common.core.resp.PageResult;
import com.dayan.common.core.resp.R;
import com.dayan.common.log.operation.OperationLog;
import com.dayan.supplier.dto.SupplierPermissionCreateDTO;
import com.dayan.supplier.dto.SupplierPermissionQueryDTO;
import com.dayan.supplier.dto.SupplierPermissionUpdateDTO;
import com.dayan.supplier.entity.SupplierPermission;
import com.dayan.supplier.service.SupplierPermissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Admin 端供应商权限管理接口（P3 简化：基础 CRUD）。
 *
 * <p>路径：{@code /supplier/permission/*}（由 dayan-admin context-path=/admin-api 拼接为
 * {@code /admin-api/supplier/permission/*}）。
 */
@Tag(name = "供应商权限管理")
@RestController
@RequestMapping("/supplier/permission")
@RequiredArgsConstructor
public class SupplierPermissionAdminController {

    private final SupplierPermissionService supplierPermissionService;

    @Operation(summary = "供应商权限分页列表")
    @SaCheckPermission("supplier:permission:list")
    @GetMapping("/page")
    public R<PageResult<SupplierPermission>> page(SupplierPermissionQueryDTO query) {
        return R.ok(supplierPermissionService.page(query));
    }

    @Operation(summary = "供应商权限全量列表（启用）")
    @SaCheckPermission("supplier:permission:list")
    @GetMapping("/list")
    public R<List<SupplierPermission>> listAll() {
        return R.ok(supplierPermissionService.listAll());
    }

    @Operation(summary = "供应商权限详情")
    @SaCheckPermission("supplier:permission:query")
    @GetMapping("/{permissionCode}")
    public R<SupplierPermission> getDetail(@PathVariable String permissionCode) {
        return R.ok(supplierPermissionService.getDetail(permissionCode));
    }

    @Operation(summary = "新增供应商权限")
    @OperationLog(module = "供应商权限", action = "新增")
    @SaCheckPermission("supplier:permission:create")
    @PostMapping
    public R<Void> create(@RequestBody @Valid SupplierPermissionCreateDTO dto) {
        supplierPermissionService.create(dto);
        return R.ok();
    }

    @Operation(summary = "修改供应商权限")
    @OperationLog(module = "供应商权限", action = "修改")
    @SaCheckPermission("supplier:permission:update")
    @PutMapping
    public R<Void> update(@RequestParam String permissionCode,
                          @RequestBody SupplierPermissionUpdateDTO dto) {
        supplierPermissionService.update(permissionCode, dto);
        return R.ok();
    }

    @Operation(summary = "删除供应商权限")
    @OperationLog(module = "供应商权限", action = "删除")
    @SaCheckPermission("supplier:permission:delete")
    @DeleteMapping("/{permissionCode}")
    public R<Void> delete(@PathVariable String permissionCode) {
        supplierPermissionService.delete(permissionCode);
        return R.ok();
    }
}
