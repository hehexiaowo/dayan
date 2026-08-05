package com.dayan.finance.controller.channel;

import com.dayan.common.core.exception.BusinessException;
import com.dayan.common.core.exception.ErrorCode;
import com.dayan.common.core.resp.R;
import com.dayan.finance.dto.CreatePaymentDTO;
import com.dayan.finance.dto.PaymentMarkSuccessDTO;
import com.dayan.finance.service.FinancePaymentService;
import com.dayan.order.service.OrderEquityService;
import com.dayan.order.vo.OrderEquityVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * Channel 渠道端支付接口。
 *
 * <p>路径：{@code /channel-api/finance-payments/*}（由 dayan-channel 启动模块的 context-path 拼接）。
 *
 * <p>防越权：支付金额从订单权威解析（{@code order_equity.pay_amount}），禁止客户端传金额（防篡改）。
 */
@Tag(name = "Channel 支付")
@RestController
@RequestMapping("/finance-payments")
@RequiredArgsConstructor
public class ChannelFinanceController {

    private final FinancePaymentService financePaymentService;
    private final OrderEquityService orderEquityService;

    @Operation(summary = "创建支付单")
    @PostMapping
    public R<String> create(@RequestBody @Valid CreatePaymentDTO dto) {
        // 权益订单（orderType=1）：从订单表权威解析 payAmount，防篡改
        if (Integer.valueOf(1).equals(dto.getOrderType())) {
            OrderEquityVO order = orderEquityService.getDetail(dto.getOrderCode());
            if (order == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND, "订单不存在");
            }
            // 覆盖客户端传入的金额（以订单表为准）
            dto.setPayAmount(order.getPayAmount());
        }
        return R.ok(financePaymentService.create(dto));
    }

    @Operation(summary = "标记支付成功")
    @PostMapping("/{paymentCode}/mark-success")
    public R<Void> markSuccess(@PathVariable String paymentCode,
                               @RequestBody @Valid PaymentMarkSuccessDTO dto) {
        // 用 path 参数覆盖 body 中的 paymentCode
        dto.setPaymentCode(paymentCode);
        financePaymentService.markSuccess(dto);
        return R.ok();
    }
}
