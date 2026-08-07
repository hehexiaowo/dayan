package com.dayan.finance.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 支付记录（finance_payment）查询入参（分页 + 多条件）。
 */
@Data
public class FinancePaymentQueryDTO {

    private Long current = 1L;
    private Long size = 20L;

    private String paymentCode;
    /** 订单类型：1=权益/2=场景/3=课程/4=旅居 */
    private Integer orderType;
    private String orderCode;
    /** 订单编码集合（多值 IN 过滤，渠道端按本渠道订单码反查支付单时使用） */
    private List<String> orderCodes;
    private String tradeNo;
    /** 支付方式 */
    private Integer payType;
    /** 支付状态：0=待支付/1=支付成功/2=支付失败/3=已退款/4=部分退款 */
    private Integer payStatus;
    /** 支付时间（大于等于） */
    private LocalDateTime payTimeFrom;
    /** 支付时间（小于等于） */
    private LocalDateTime payTimeTo;
}
