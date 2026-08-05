package com.dayan.supplier.controller.admin;

import com.dayan.common.core.resp.PageResult;
import com.dayan.common.core.resp.R;
import com.dayan.common.log.operation.OperationLog;
import com.dayan.supplier.dto.SupplierAuditDTO;
import com.dayan.supplier.dto.SupplierInfoCreateDTO;
import com.dayan.supplier.dto.SupplierInfoQueryDTO;
import com.dayan.supplier.dto.SupplierInfoUpdateDTO;
import com.dayan.supplier.service.SupplierInfoService;
import com.dayan.supplier.vo.SupplierInfoVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * Admin 端供应商信息接口（审核流）。
 *
 * <p>路径：{@code /supplier/info/*}（由 dayan-admin 启动模块 context-path=/admin-api 拼接为
 * {@code /admin-api/supplier/info/*}）。
 */
@Tag(name = "供应商信息管理")
@RestController
@RequestMapping("/supplier/info")
@RequiredArgsConstructor
public class SupplierInfoAdminController {

    private final SupplierInfoService supplierInfoService;

    @Operation(summary = "供应商分页列表")
    @SaCheckPermission("supplier:info:list")
    @GetMapping("/page")
    public R<PageResult<SupplierInfoVO>> page(SupplierInfoQueryDTO query) {
        return R.ok(supplierInfoService.page(query));
    }

    @Operation(summary = "供应商列表（全量）")
    @SaCheckPermission("supplier:info:list")
    @GetMapping("/list")
    public R<PageResult<SupplierInfoVO>> list(SupplierInfoQueryDTO query) {
        query.setSize(1000L);
        return R.ok(supplierInfoService.page(query));
    }

    @Operation(summary = "供应商详情")
    @SaCheckPermission("supplier:info:query")
    @GetMapping("/{supplierCode}")
    public R<SupplierInfoVO> getDetail(@PathVariable String supplierCode) {
        return R.ok(supplierInfoService.getDetail(supplierCode));
    }

    @Operation(summary = "新增供应商")
    @OperationLog(module = "供应商信息", action = "新增")
    @SaCheckPermission("supplier:info:create")
    @PostMapping
    public R<String> create(@RequestBody @Valid SupplierInfoCreateDTO dto) {
        return R.ok(supplierInfoService.create(dto));
    }

    @Operation(summary = "修改供应商")
    @OperationLog(module = "供应商信息", action = "修改")
    @SaCheckPermission("supplier:info:update")
    @PutMapping
    public R<Void> update(@RequestParam String supplierCode,
                          @RequestBody SupplierInfoUpdateDTO dto) {
        supplierInfoService.update(supplierCode, dto);
        return R.ok();
    }

    @Operation(summary = "审核供应商")
    @OperationLog(module = "供应商信息", action = "审核")
    @SaCheckPermission("supplier:info:audit")
    @PostMapping("/audit")
    public R<Void> audit(@RequestBody @Valid SupplierAuditDTO dto) {
        supplierInfoService.audit(dto);
        return R.ok();
    }

    @Operation(summary = "删除供应商")
    @OperationLog(module = "供应商信息", action = "删除")
    @SaCheckPermission("supplier:info:delete")
    @DeleteMapping("/{supplierCode}")
    public R<Void> delete(@PathVariable String supplierCode) {
        supplierInfoService.delete(supplierCode);
        return R.ok();
    }
}
