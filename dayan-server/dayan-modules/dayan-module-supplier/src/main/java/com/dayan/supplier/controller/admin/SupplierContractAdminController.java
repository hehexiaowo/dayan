package com.dayan.supplier.controller.admin;

import com.dayan.common.core.resp.PageResult;
import com.dayan.common.core.resp.R;
import com.dayan.common.log.operation.OperationLog;
import com.dayan.supplier.dto.SupplierContractCreateDTO;
import com.dayan.supplier.dto.SupplierContractQueryDTO;
import com.dayan.supplier.dto.SupplierContractUpdateDTO;
import com.dayan.supplier.service.SupplierContractService;
import com.dayan.supplier.vo.SupplierContractVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * Admin 端供应商合同管理接口（续约链）。
 *
 * <p>路径：{@code /supplier/contract/*}（由 dayan-admin context-path=/admin-api 拼接为
 * {@code /admin-api/supplier/contract/*}）。
 */
@Tag(name = "供应商合同管理")
@RestController
@RequestMapping("/supplier/contract")
@RequiredArgsConstructor
public class SupplierContractAdminController {

    private final SupplierContractService supplierContractService;

    @Operation(summary = "供应商合同分页列表")
    @GetMapping("/page")
    public R<PageResult<SupplierContractVO>> page(SupplierContractQueryDTO query) {
        return R.ok(supplierContractService.page(query));
    }

    @Operation(summary = "供应商合同详情")
    @GetMapping("/{contractCode}")
    public R<SupplierContractVO> getDetail(@PathVariable String contractCode) {
        return R.ok(supplierContractService.getDetail(contractCode));
    }

    @Operation(summary = "新增供应商合同（含续约：传 parentContractCode）")
    @OperationLog(module = "供应商合同", action = "新增")
    @PostMapping
    public R<String> create(@RequestBody @Valid SupplierContractCreateDTO dto) {
        return R.ok(supplierContractService.create(dto));
    }

    @Operation(summary = "修改供应商合同")
    @OperationLog(module = "供应商合同", action = "修改")
    @PutMapping
    public R<Void> update(@RequestParam String contractCode,
                          @RequestBody SupplierContractUpdateDTO dto) {
        supplierContractService.update(contractCode, dto);
        return R.ok();
    }

    @Operation(summary = "删除供应商合同")
    @OperationLog(module = "供应商合同", action = "删除")
    @DeleteMapping("/{contractCode}")
    public R<Void> delete(@PathVariable String contractCode) {
        supplierContractService.delete(contractCode);
        return R.ok();
    }
}
