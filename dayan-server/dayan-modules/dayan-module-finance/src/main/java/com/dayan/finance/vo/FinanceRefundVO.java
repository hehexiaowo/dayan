package com.dayan.finance.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单退款记录（finance_refund）视图对象。
 */
@Data
public class FinanceRefundVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private String refundCode;
    /** 订单类型：1=权益/2=场景/3=课程/4=旅游短居 */
    private Integer orderType;
    private String orderCode;
    private String paymentCode;
    private BigDecimal refundAmount;
    private String refundReason;
    /** 退款类型：1=全额退款/2=部分退款 */
    private Integer refundType;
    /** 退款渠道：1=原路退回/2=退到余额/3=线下退款 */
    private Integer refundChannel;
    private String refundTradeNo;
    private LocalDateTime applyTime;
    private LocalDateTime auditTime;
    private LocalDateTime refundTime;
    private String auditorCode;
    private String auditorName;
    private String auditRemark;
    /** 状态：0=待审核/1=审核通过/2=退款中/3=退款成功/4=审核拒绝/5=退款失败 */
    private Integer refundStatus;
    private String remark;
    private LocalDateTime createdAt;
}
