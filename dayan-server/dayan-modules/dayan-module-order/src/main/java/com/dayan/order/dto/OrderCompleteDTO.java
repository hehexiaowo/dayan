package com.dayan.order.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 完成订单入参（通用，适用于 4 类订单）。
 *
 * <p>状态机流转：3(已发放) --complete--> 4(已完成)。
 */
@Data
public class OrderCompleteDTO {

    /** 订单编号 */
    @NotBlank(message = "订单编号不能为空")
    private String orderCode;

    /** 操作人编码（写入日志，可空默认 system） */
    private String operatorCode;
    /** 操作人姓名（写入日志，可空） */
    private String operatorName;
    /** 操作人类型（admin/system/channel 等，可空默认 system） */
    private String operatorType;
}
