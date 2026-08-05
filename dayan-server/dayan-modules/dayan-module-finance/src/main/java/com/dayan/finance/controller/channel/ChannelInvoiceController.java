package com.dayan.finance.controller.channel;

import com.dayan.common.core.resp.R;
import com.dayan.common.mybatis.context.ContextHolder;
import com.dayan.finance.dto.ApplyInvoiceDTO;
import com.dayan.finance.service.FinanceInvoiceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * Channel 渠道端发票申请接口。
 *
 * <p>路径：{@code /channel-api/finance-invoices/*}（由 dayan-channel 启动模块的 context-path 拼接）。
 */
@Tag(name = "Channel 发票申请")
@RestController
@RequestMapping("/finance-invoices")
@RequiredArgsConstructor
public class ChannelInvoiceController {

    private final FinanceInvoiceService financeInvoiceService;

    @Operation(summary = "渠道申请发票")
    @PostMapping("/apply")
    public R<String> apply(@RequestBody @Valid ApplyInvoiceDTO dto) {
        // 强制注入申请方信息，防止越权
        dto.setApplicantCode(ContextHolder.getChannelCode());
        dto.setApplicantType("channel");
        return R.ok(financeInvoiceService.apply(dto));
    }
}
