package com.dayan.finance.controller.admin;

import com.dayan.common.core.resp.PageResult;
import com.dayan.common.core.resp.R;
import com.dayan.finance.dto.ApplyInvoiceDTO;
import com.dayan.finance.dto.FinanceInvoiceQueryDTO;
import com.dayan.finance.dto.InvoiceAuditDTO;
import com.dayan.finance.dto.InvoiceIssueDTO;
import com.dayan.finance.dto.InvoiceOperateDTO;
import com.dayan.finance.dto.InvoiceSendDTO;
import com.dayan.finance.service.FinanceInvoiceService;
import com.dayan.finance.vo.FinanceInvoiceVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Admin 端发票接口。
 *
 * <p>路径前缀 {@code /finance/invoice}。
 */
@Tag(name = "发票管理")
@RestController
@RequestMapping("/finance/invoice")
@RequiredArgsConstructor
public class FinanceInvoiceAdminController {

    private final FinanceInvoiceService financeInvoiceService;

    @Operation(summary = "发票分页列表")
    @GetMapping("/page")
    public R<PageResult<FinanceInvoiceVO>> page(FinanceInvoiceQueryDTO query) {
        return R.ok(financeInvoiceService.page(query));
    }

    @Operation(summary = "发票列表（全量）")
    @GetMapping("/list")
    public R<List<FinanceInvoiceVO>> list(FinanceInvoiceQueryDTO query) {
        return R.ok(financeInvoiceService.list(query));
    }

    @Operation(summary = "发票详情")
    @GetMapping("/{invoiceCode}")
    public R<FinanceInvoiceVO> getDetail(@PathVariable String invoiceCode) {
        return R.ok(financeInvoiceService.getDetail(invoiceCode));
    }

    @Operation(summary = "申请发票")
    @PostMapping("/apply")
    public R<String> apply(@RequestBody @Valid ApplyInvoiceDTO dto) {
        return R.ok(financeInvoiceService.apply(dto));
    }

    @Operation(summary = "审核发票（0→1）")
    @PostMapping("/audit")
    public R<Void> audit(@RequestBody @Valid InvoiceAuditDTO dto) {
        financeInvoiceService.audit(dto);
        return R.ok();
    }

    @Operation(summary = "开具发票（1→2）")
    @PostMapping("/issue")
    public R<Void> issue(@RequestBody @Valid InvoiceIssueDTO dto) {
        financeInvoiceService.issue(dto);
        return R.ok();
    }

    @Operation(summary = "寄出发票（2→3）")
    @PostMapping("/send")
    public R<Void> send(@RequestBody @Valid InvoiceSendDTO dto) {
        financeInvoiceService.send(dto);
        return R.ok();
    }

    @Operation(summary = "完成发票（3→4）")
    @PostMapping("/finish")
    public R<Void> finish(@RequestBody @Valid InvoiceOperateDTO dto) {
        financeInvoiceService.finish(dto);
        return R.ok();
    }

    @Operation(summary = "作废发票（→5）")
    @PostMapping("/void")
    public R<Void> voidInvoice(@RequestBody @Valid InvoiceOperateDTO dto) {
        financeInvoiceService.voidInvoice(dto);
        return R.ok();
    }

    @Operation(summary = "红冲发票（→6）")
    @PostMapping("/red-flush")
    public R<Void> redFlush(@RequestBody @Valid InvoiceOperateDTO dto) {
        financeInvoiceService.redFlush(dto);
        return R.ok();
    }
}
