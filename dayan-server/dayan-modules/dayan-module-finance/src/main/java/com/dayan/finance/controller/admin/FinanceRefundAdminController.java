package com.dayan.finance.controller.admin;

import com.dayan.common.core.resp.PageResult;
import com.dayan.common.core.resp.R;
import com.dayan.common.log.operation.OperationLog;
import com.dayan.finance.dto.ApplyRefundDTO;
import com.dayan.finance.dto.FinanceRefundQueryDTO;
import com.dayan.finance.dto.RefundAuditDTO;
import com.dayan.finance.dto.RefundMarkFailedDTO;
import com.dayan.finance.dto.RefundMarkSuccessDTO;
import com.dayan.finance.service.FinanceRefundService;
import com.dayan.finance.vo.FinanceRefundVO;
import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Admin 端退款记录接口。
 *
 * <p>路径前缀 {@code /finance/refund}。
 */
@Tag(name = "退款记录管理")
@RestController
@RequestMapping("/finance/refund")
@RequiredArgsConstructor
public class FinanceRefundAdminController {

    private final FinanceRefundService financeRefundService;

    @Operation(summary = "退款记录分页列表")
    @SaCheckPermission("finance:refund:list")
    @GetMapping("/page")
    public R<PageResult<FinanceRefundVO>> page(FinanceRefundQueryDTO query) {
        return R.ok(financeRefundService.page(query));
    }

    @Operation(summary = "退款记录列表（全量）")
    @SaCheckPermission("finance:refund:list")
    @GetMapping("/list")
    public R<List<FinanceRefundVO>> list(FinanceRefundQueryDTO query) {
        return R.ok(financeRefundService.list(query));
    }

    @Operation(summary = "退款记录详情")
    @SaCheckPermission("finance:refund:query")
    @GetMapping("/{refundCode}")
    public R<FinanceRefundVO> getDetail(@PathVariable String refundCode) {
        return R.ok(financeRefundService.getDetail(refundCode));
    }

    @Operation(summary = "申请退款")
    @OperationLog(module = "财务退款", action = "申请")
    @SaCheckPermission("finance:refund:apply")
    @PostMapping("/apply")
    public R<String> apply(@RequestBody @Valid ApplyRefundDTO dto) {
        return R.ok(financeRefundService.apply(dto));
    }

    @Operation(summary = "审核退款（0→1 通过 / 0→4 拒绝）")
    @OperationLog(module = "财务退款", action = "审核")
    @SaCheckPermission("finance:refund:audit")
    @PostMapping("/audit")
    public R<Void> audit(@RequestBody @Valid RefundAuditDTO dto) {
        financeRefundService.audit(dto);
        return R.ok();
    }

    @Operation(summary = "进入退款中（1→2）")
    @OperationLog(module = "财务退款", action = "标记退款中")
    @SaCheckPermission("finance:refund:mark-refunding")
    @PostMapping("/mark-refunding/{refundCode}")
    public R<Void> markRefunding(@PathVariable String refundCode) {
        financeRefundService.markRefunding(refundCode);
        return R.ok();
    }

    @Operation(summary = "标记退款成功（2→3）")
    @OperationLog(module = "财务退款", action = "标记成功")
    @SaCheckPermission("finance:refund:mark-success")
    @PostMapping("/mark-success")
    public R<Void> markSuccess(@RequestBody @Valid RefundMarkSuccessDTO dto) {
        financeRefundService.markSuccess(dto);
        return R.ok();
    }

    @Operation(summary = "标记退款失败（2→5）")
    @OperationLog(module = "财务退款", action = "标记失败")
    @SaCheckPermission("finance:refund:mark-failed")
    @PostMapping("/mark-failed")
    public R<Void> markFailed(@RequestBody @Valid RefundMarkFailedDTO dto) {
        financeRefundService.markFailed(dto);
        return R.ok();
    }
}
