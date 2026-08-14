package com.dayan.finance.controller.admin;

import com.dayan.common.core.resp.PageResult;
import com.dayan.common.core.resp.R;
import com.dayan.common.log.operation.OperationLog;
import com.dayan.finance.dto.CreateReconciliationDTO;
import com.dayan.finance.dto.FinanceReconciliationQueryDTO;
import com.dayan.finance.dto.ReconciliationConfirmDTO;
import com.dayan.finance.dto.ReconciliationSubmitDiffDTO;
import com.dayan.finance.service.FinanceReconciliationService;
import com.dayan.finance.vo.FinanceReconciliationVO;
import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Admin 端对账记录接口。
 *
 * <p>路径前缀 {@code /finance/reconciliation}。
 */
@Tag(name = "对账记录管理")
@RestController
@RequestMapping("/finance/reconciliation")
@RequiredArgsConstructor
public class FinanceReconciliationAdminController {

    private final FinanceReconciliationService financeReconciliationService;

    @Operation(summary = "对账记录分页列表")
    @SaCheckPermission("finance:reconciliation:list")
    @GetMapping("/page")
    public R<PageResult<FinanceReconciliationVO>> page(FinanceReconciliationQueryDTO query) {
        return R.ok(financeReconciliationService.page(query));
    }

    @Operation(summary = "对账记录列表（全量）")
    @SaCheckPermission("finance:reconciliation:list")
    @GetMapping("/list")
    public R<List<FinanceReconciliationVO>> list(FinanceReconciliationQueryDTO query) {
        return R.ok(financeReconciliationService.list(query));
    }

    @Operation(summary = "对账记录详情")
    @SaCheckPermission("finance:reconciliation:query")
    @GetMapping("/{reconCode}")
    public R<FinanceReconciliationVO> getDetail(@PathVariable String reconCode) {
        return R.ok(financeReconciliationService.getDetail(reconCode));
    }

    @Operation(summary = "创建对账记录")
    @OperationLog(module = "财务对账", action = "新增")
    @SaCheckPermission("finance:reconciliation:create")
    @PostMapping
    public R<String> create(@RequestBody @Valid CreateReconciliationDTO dto) {
        return R.ok(financeReconciliationService.create(dto));
    }

    @Operation(summary = "完成对账（0→1，无差异）")
    @OperationLog(module = "财务对账", action = "完成对账")
    @SaCheckPermission("finance:reconciliation:complete")
    @PostMapping("/complete/{reconCode}")
    public R<Void> complete(@PathVariable String reconCode) {
        financeReconciliationService.complete(reconCode);
        return R.ok();
    }

    @Operation(summary = "提交差异（0→2 待确认）")
    @OperationLog(module = "财务对账", action = "提交差异")
    @SaCheckPermission("finance:reconciliation:submit-diff")
    @PostMapping("/submit-diff")
    public R<Void> submitDiff(@RequestBody @Valid ReconciliationSubmitDiffDTO dto) {
        financeReconciliationService.submitDiff(dto);
        return R.ok();
    }

    @Operation(summary = "确认对账（2→3 已确认）")
    @OperationLog(module = "财务对账", action = "确认")
    @SaCheckPermission("finance:reconciliation:confirm")
    @PostMapping("/confirm")
    public R<Void> confirm(@RequestBody @Valid ReconciliationConfirmDTO dto) {
        financeReconciliationService.confirm(dto);
        return R.ok();
    }
}
