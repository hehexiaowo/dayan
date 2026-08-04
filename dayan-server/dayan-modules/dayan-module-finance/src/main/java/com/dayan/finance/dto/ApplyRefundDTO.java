package com.dayan.finance.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 申请退款（finance_refund）入参。
 *
 * <p>退款编号由服务端生成（RF+序号），refund_status=0（待审核）+apply_time=now。
 */
@Data
public class ApplyRefundDTO {

    /** 订单类型：1=权益/2=场景/3=课程/4=旅居 */
    @NotNull(message = "订单类型不能为空")
    private Integer orderType;

    /** 订单编号 */
    @NotBlank(message = "订单编号不能为空")
    private String orderCode;

    /** 原支付记录编码（可空，线下退款可无） */
    private String paymentCode;

    /** 退款金额 */
    @NotNull(message = "退款金额不能为空")
    private BigDecimal refundAmount;

    /** 退款原因 */
    @NotBlank(message = "退款原因不能为空")
    private String refundReason;

    /** 退款类型：1=全额退款/2=部分退款（默认 1） */
    private Integer refundType;

    /** 退款渠道：1=原路退回/2=退到余额/3=线下退款（默认 1） */
    private Integer refundChannel;

    /** 备注 */
    private String remark;
}
