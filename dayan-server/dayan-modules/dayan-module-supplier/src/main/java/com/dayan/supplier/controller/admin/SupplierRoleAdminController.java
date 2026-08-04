package com.dayan.supplier.controller.admin;

import com.dayan.common.core.resp.PageResult;
import com.dayan.common.core.resp.R;
import com.dayan.common.log.operation.OperationLog;
import com.dayan.supplier.dto.SupplierRoleCreateDTO;
import com.dayan.supplier.dto.SupplierRoleQueryDTO;
import com.dayan.supplier.dto.SupplierRoleUpdateDTO;
import com.dayan.supplier.entity.SupplierRole;
import com.dayan.supplier.service.SupplierRoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Admin 端供应商角色管理接口（P3 简化：基础 CRUD + 授权）。
 *
 * <p>路径：{@code /supplier/role/*}（由 dayan-admin context-path=/admin-api 拼接为
 * {@code /admin-api/supplier/role/*}）。RBAC 鉴权回调后置实现。
 */
@Tag(name = "供应商角色管理")
@RestController
@RequestMapping("/supplier/role")
@RequiredArgsConstructor
public class SupplierRoleAdminController {

    private final SupplierRoleService supplierRoleService;

    @Operation(summary = "供应商角色分页列表")
    @GetMapping("/page")
    public R<PageResult<SupplierRole>> page(SupplierRoleQueryDTO query) {
        return R.ok(supplierRoleService.page(query));
    }

    @Operation(summary = "供应商角色详情")
    @GetMapping("/{roleCode}")
    public R<SupplierRole> getDetail(@PathVariable String roleCode) {
        return R.ok(supplierRoleService.getDetail(roleCode));
    }

    @Operation(summary = "新增供应商角色")
    @OperationLog(module = "供应商角色", action = "新增")
    @PostMapping
    public R<String> create(@RequestBody @Valid SupplierRoleCreateDTO dto) {
        return R.ok(supplierRoleService.create(dto));
    }

    @Operation(summary = "修改供应商角色")
    @OperationLog(module = "供应商角色", action = "修改")
    @PutMapping
    public R<Void> update(@RequestParam String roleCode,
                          @RequestBody SupplierRoleUpdateDTO dto) {
        supplierRoleService.update(roleCode, dto);
        return R.ok();
    }

    @Operation(summary = "删除供应商角色")
    @OperationLog(module = "供应商角色", action = "删除")
    @DeleteMapping("/{roleCode}")
    public R<Void> delete(@PathVariable String roleCode) {
        supplierRoleService.delete(roleCode);
        return R.ok();
    }

    @Operation(summary = "给供应商角色授权（全量覆盖）")
    @OperationLog(module = "供应商角色", action = "授权")
    @PutMapping("/{roleCode}/permissions")
    public R<Void> assignPermissions(@PathVariable String roleCode,
                                     @RequestBody List<String> permissionCodes) {
        supplierRoleService.assignPermissions(roleCode, permissionCodes);
        return R.ok();
    }

    @Operation(summary = "查询供应商角色权限码列表")
    @GetMapping("/{roleCode}/permissions")
    public R<List<String>> listPermissions(@PathVariable String roleCode) {
        return R.ok(supplierRoleService.listPermissions(roleCode));
    }
}
