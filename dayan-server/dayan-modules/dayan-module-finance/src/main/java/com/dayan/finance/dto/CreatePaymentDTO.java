package com.dayan.finance.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 创建支付记录（finance_payment）入参。
 *
 * <p>支付流水号由服务端生成（PAY+序号），pay_status 置 0（待支付）。
 */
@Data
public class CreatePaymentDTO {

    /** 订单类型：1=权益/2=场景/3=课程/4=旅游短居 */
    @NotNull(message = "订单类型不能为空")
    private Integer orderType;

    /** 订单编号 */
    @NotBlank(message = "订单编号不能为空")
    private String orderCode;

    /** 支付方式：1=微信/2=支付宝/3=银行转账/4=余额/5=线下 */
    @NotNull(message = "支付方式不能为空")
    private Integer payType;

    /** 支付金额（Channel 端下单时由订单权威解析，可不传） */
    private BigDecimal payAmount;

    /** 付款方账号（可空） */
    private String payerAccount;
    /** 收款方账号（可空） */
    private String payeeAccount;

    /** 支付说明 */
    private String payDescription;

    /** 扩展数据（JSON 字符串） */
    private String extraData;
}
