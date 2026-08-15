package com.dayan.finance.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单支付记录（finance_payment）视图对象。
 */
@Data
public class FinancePaymentVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private String paymentCode;
    /** 订单类型：1=权益/2=场景/3=课程/4=旅游短居 */
    private Integer orderType;
    private String orderCode;
    /** 支付方式：1=微信/2=支付宝/3=银行转账/4=余额/5=线下 */
    private Integer payType;
    private BigDecimal payAmount;
    private String tradeNo;
    private String payerAccount;
    private String payeeAccount;
    private LocalDateTime payTime;
    private LocalDateTime notifyTime;
    /** 支付状态：0=待支付/1=支付成功/2=支付失败/3=已退款/4=部分退款 */
    private Integer payStatus;
    private String payDescription;
    private String extraData;
    private LocalDateTime createdAt;
}
