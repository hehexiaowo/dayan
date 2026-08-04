package com.dayan.order.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 权益订单发货入参（order_equity 专用）。
 *
 * <p>状态机流转：
 * <ul>
 *   <li>{@code partialDeliver=true}：1(已支付) --partial_deliver--> 2(部分发放)，更新 deliverCount += 本次数量</li>
 *   <li>{@code partialDeliver=false}：1/2 --deliver--> 3(已发放)，deliverCount = quantity，写 deliverTime</li>
 * </ul>
 * 本期不强校验 equity 域出库（跨域，仅维护订单侧状态）。
 */
@Data
public class EquityDeliverDTO {

    /** 订单编号 */
    @NotBlank(message = "订单编号不能为空")
    private String orderCode;

    /** 本次发放数量（partialDeliver=true 时累加到 deliverCount） */
    @NotNull(message = "发放数量不能为空")
    @Min(value = 1, message = "发放数量必须大于 0")
    private Integer deliverCount;

    /** 是否部分发放：true=部分发放(1→2)，false=全部发放完成(1/2→3) */
    private Boolean partialDeliver = false;

    /** 操作人编码（写入日志，可空默认 system） */
    private String operatorCode;
    /** 操作人姓名（写入日志，可空） */
    private String operatorName;
    /** 操作人类型（admin/system/channel 等，可空默认 system） */
    private String operatorType;
}
