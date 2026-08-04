package com.dayan.order.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 支付回调入参（通用，适用于 4 类订单）。
 *
 * <p>状态机流转：0(待支付) --pay--> 1(已支付)。写 pay_time/pay_trade_no/pay_type。
 */
@Data
public class PayCallbackDTO {

    /** 订单编号 */
    @NotBlank(message = "订单编号不能为空")
    private String orderCode;

    /** 支付流水号（第三方交易号） */
    @NotBlank(message = "支付流水号不能为空")
    private String payTradeNo;

    /** 支付方式：1=微信 / 2=支付宝 / 3=银行转账 / 4=余额 / 5=线下 */
    private Integer payType;

    /** 操作人编码（写入日志，可空默认 system） */
    private String operatorCode;
    /** 操作人姓名（写入日志，可空） */
    private String operatorName;
    /** 操作人类型（admin/system/channel 等，可空默认 system） */
    private String operatorType;
}
