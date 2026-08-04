package com.dayan.finance.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 退款记录（finance_refund）查询入参（分页 + 多条件）。
 */
@Data
public class FinanceRefundQueryDTO {

    private Long current = 1L;
    private Long size = 20L;

    private String refundCode;
    /** 订单类型 */
    private Integer orderType;
    private String orderCode;
    private String paymentCode;
    /** 退款类型：1=全额退款/2=部分退款 */
    private Integer refundType;
    /** 退款渠道：1=原路退回/2=退到余额/3=线下退款 */
    private Integer refundChannel;
    /** 状态：0=待审核/1=审核通过/2=退款中/3=退款成功/4=审核拒绝/5=退款失败 */
    private Integer refundStatus;
    /** 申请时间（大于等于） */
    private LocalDateTime applyTimeFrom;
    /** 申请时间（小于等于） */
    private LocalDateTime applyTimeTo;
}
