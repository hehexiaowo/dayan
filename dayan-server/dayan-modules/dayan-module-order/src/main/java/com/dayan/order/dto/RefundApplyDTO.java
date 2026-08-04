package com.dayan.order.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 申请退款入参（通用，适用于 4 类订单）。
 *
 * <p>状态机流转：1/2/3 --refund_apply--> 6(退款中)。退款金额/渠道等由结算域 finance_refund 独立维护，
 * 订单域仅推进订单状态。
 */
@Data
public class RefundApplyDTO {

    /** 订单编号 */
    @NotBlank(message = "订单编号不能为空")
    private String orderCode;

    /** 退款原因 */
    @NotBlank(message = "退款原因不能为空")
    private String refundReason;

    /** 操作人编码（写入日志，可空默认 system） */
    private String operatorCode;
    /** 操作人姓名（写入日志，可空） */
    private String operatorName;
    /** 操作人类型（admin/system/channel 等，可空默认 system） */
    private String operatorType;
}
