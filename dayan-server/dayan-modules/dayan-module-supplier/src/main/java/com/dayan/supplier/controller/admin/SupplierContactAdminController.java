package com.dayan.supplier.controller.admin;

import com.dayan.common.core.resp.PageResult;
import com.dayan.common.core.resp.R;
import com.dayan.common.log.operation.OperationLog;
import com.dayan.supplier.dto.SupplierContactCreateDTO;
import com.dayan.supplier.dto.SupplierContactQueryDTO;
import com.dayan.supplier.dto.SupplierContactUpdateDTO;
import com.dayan.supplier.service.SupplierContactService;
import com.dayan.supplier.vo.SupplierContactVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * Admin 端供应商联系人管理接口。
 *
 * <p>路径：{@code /supplier/contact/*}（由 dayan-admin context-path=/admin-api 拼接为
 * {@code /admin-api/supplier/contact/*}）。
 */
@Tag(name = "供应商联系人管理")
@RestController
@RequestMapping("/supplier/contact")
@RequiredArgsConstructor
public class SupplierContactAdminController {

    private final SupplierContactService supplierContactService;

    @Operation(summary = "供应商联系人分页列表")
    @GetMapping("/page")
    public R<PageResult<SupplierContactVO>> page(SupplierContactQueryDTO query) {
        return R.ok(supplierContactService.page(query));
    }

    @Operation(summary = "供应商联系人详情")
    @GetMapping("/{id}")
    public R<SupplierContactVO> getDetail(@PathVariable Long id) {
        return R.ok(supplierContactService.getDetail(id));
    }

    @Operation(summary = "新增供应商联系人")
    @OperationLog(module = "供应商联系人", action = "新增")
    @PostMapping
    public R<Long> create(@RequestBody @Valid SupplierContactCreateDTO dto) {
        return R.ok(supplierContactService.create(dto));
    }

    @Operation(summary = "修改供应商联系人")
    @OperationLog(module = "供应商联系人", action = "修改")
    @PutMapping
    public R<Void> update(@RequestParam Long id,
                          @RequestBody SupplierContactUpdateDTO dto) {
        supplierContactService.update(id, dto);
        return R.ok();
    }

    @Operation(summary = "删除供应商联系人")
    @OperationLog(module = "供应商联系人", action = "删除")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        supplierContactService.delete(id);
        return R.ok();
    }
}
