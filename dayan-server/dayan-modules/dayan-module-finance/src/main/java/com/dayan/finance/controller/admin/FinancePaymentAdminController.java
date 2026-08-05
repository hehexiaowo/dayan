package com.dayan.finance.controller.admin;

import com.dayan.common.core.resp.PageResult;
import com.dayan.common.core.resp.R;
import com.dayan.finance.dto.CreatePaymentDTO;
import com.dayan.finance.dto.FinancePaymentQueryDTO;
import com.dayan.finance.dto.PaymentMarkFailedDTO;
import com.dayan.finance.dto.PaymentMarkSuccessDTO;
import com.dayan.finance.service.FinancePaymentService;
import com.dayan.finance.vo.FinancePaymentVO;
import cn.dev33.satoken.annotation.SaCheckPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Admin 端支付记录接口。
 *
 * <p>路径前缀 {@code /finance/payment}。
 */
@Tag(name = "支付记录管理")
@RestController
@RequestMapping("/finance/payment")
@RequiredArgsConstructor
public class FinancePaymentAdminController {

    private final FinancePaymentService financePaymentService;

    @Operation(summary = "支付记录分页列表")
    @SaCheckPermission("finance:payment:list")
    @GetMapping("/page")
    public R<PageResult<FinancePaymentVO>> page(FinancePaymentQueryDTO query) {
        return R.ok(financePaymentService.page(query));
    }

    @Operation(summary = "支付记录列表（全量）")
    @SaCheckPermission("finance:payment:list")
    @GetMapping("/list")
    public R<List<FinancePaymentVO>> list(FinancePaymentQueryDTO query) {
        return R.ok(financePaymentService.list(query));
    }

    @Operation(summary = "支付记录详情")
    @SaCheckPermission("finance:payment:query")
    @GetMapping("/{paymentCode}")
    public R<FinancePaymentVO> getDetail(@PathVariable String paymentCode) {
        return R.ok(financePaymentService.getDetail(paymentCode));
    }

    @Operation(summary = "创建支付记录")
    @SaCheckPermission("finance:payment:create")
    @PostMapping
    public R<String> create(@RequestBody @Valid CreatePaymentDTO dto) {
        return R.ok(financePaymentService.create(dto));
    }

    @Operation(summary = "标记支付成功（0→1）")
    @SaCheckPermission("finance:payment:mark-success")
    @PostMapping("/mark-success")
    public R<Void> markSuccess(@RequestBody @Valid PaymentMarkSuccessDTO dto) {
        financePaymentService.markSuccess(dto);
        return R.ok();
    }

    @Operation(summary = "标记支付失败（0→2）")
    @SaCheckPermission("finance:payment:mark-failed")
    @PostMapping("/mark-failed")
    public R<Void> markFailed(@RequestBody @Valid PaymentMarkFailedDTO dto) {
        financePaymentService.markFailed(dto);
        return R.ok();
    }
}
