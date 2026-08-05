package com.dayan.supplier.controller.admin;

import com.dayan.common.core.resp.PageResult;
import com.dayan.common.core.resp.R;
import com.dayan.common.log.operation.OperationLog;
import com.dayan.supplier.dto.SupplierEvaluationCreateDTO;
import com.dayan.supplier.dto.SupplierEvaluationQueryDTO;
import com.dayan.supplier.dto.SupplierEvaluationUpdateDTO;
import com.dayan.supplier.service.SupplierEvaluationService;
import com.dayan.supplier.vo.SupplierEvaluationVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * Admin 端供应商评估管理接口。
 *
 * <p>路径：{@code /supplier/evaluation/*}（由 dayan-admin context-path=/admin-api 拼接为
 * {@code /admin-api/supplier/evaluation/*}）。
 */
@Tag(name = "供应商评估管理")
@RestController
@RequestMapping("/supplier/evaluation")
@RequiredArgsConstructor
public class SupplierEvaluationAdminController {

    private final SupplierEvaluationService supplierEvaluationService;

    @Operation(summary = "供应商评估分页列表")
    @SaCheckPermission("supplier:evaluation:list")
    @GetMapping("/page")
    public R<PageResult<SupplierEvaluationVO>> page(SupplierEvaluationQueryDTO query) {
        return R.ok(supplierEvaluationService.page(query));
    }

    @Operation(summary = "供应商评估详情")
    @SaCheckPermission("supplier:evaluation:query")
    @GetMapping("/{id}")
    public R<SupplierEvaluationVO> getDetail(@PathVariable Long id) {
        return R.ok(supplierEvaluationService.getDetail(id));
    }

    @Operation(summary = "新增供应商评估（totalScore/scoreLevel 自动计算）")
    @OperationLog(module = "供应商评估", action = "新增")
    @SaCheckPermission("supplier:evaluation:create")
    @PostMapping
    public R<Long> create(@RequestBody @Valid SupplierEvaluationCreateDTO dto) {
        return R.ok(supplierEvaluationService.create(dto));
    }

    @Operation(summary = "修改供应商评估")
    @OperationLog(module = "供应商评估", action = "修改")
    @SaCheckPermission("supplier:evaluation:update")
    @PutMapping
    public R<Void> update(@RequestParam Long id,
                          @RequestBody SupplierEvaluationUpdateDTO dto) {
        supplierEvaluationService.update(id, dto);
        return R.ok();
    }

    @Operation(summary = "删除供应商评估")
    @OperationLog(module = "供应商评估", action = "删除")
    @SaCheckPermission("supplier:evaluation:delete")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        supplierEvaluationService.delete(id);
        return R.ok();
    }
}
