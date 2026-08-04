package com.dayan.order.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 取消订单入参（通用，适用于 4 类订单）。
 *
 * <p>状态机流转：0(待支付) --cancel--> 5(已取消)；6(退款中) --cancel--> 5(已取消)。写 cancel_reason。
 */
@Data
public class OrderCancelDTO {

    /** 订单编号 */
    @NotBlank(message = "订单编号不能为空")
    private String orderCode;

    /** 取消原因 */
    @NotBlank(message = "取消原因不能为空")
    private String cancelReason;

    /** 操作人编码（写入日志，可空默认 system） */
    private String operatorCode;
    /** 操作人姓名（写入日志，可空） */
    private String operatorName;
    /** 操作人类型（admin/system/channel 等，可空默认 system） */
    private String operatorType;
}
