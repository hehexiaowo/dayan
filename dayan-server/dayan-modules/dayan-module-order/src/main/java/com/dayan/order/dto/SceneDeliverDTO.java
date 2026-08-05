package com.dayan.order.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 场景订单核销（发货）入参。
 *
 * <p>状态机流转：1(已支付) --deliver--> 3(已发放/已核销)。
 * 场景订单无"部分发货"概念，到场核销即视为全部履约完成。
 * 核销后可通过 {@code complete(3→4)} 完结订单。
 */
@Data
public class SceneDeliverDTO {

    /** 订单编号 */
    @NotBlank(message = "订单编号不能为空")
    private String orderCode;

    /** 核销人编码（如管家/工作人员，可空） */
    private String verifierCode;

    /** 核销人姓名（可空） */
    private String verifierName;

    /** 实际到场人数（可空，默认取订单 participantCount） */
    private Integer actualCount;

    /** 核销备注（可空） */
    private String remark;

    /** 操作人编码（写入日志，可空默认 system） */
    private String operatorCode;
    /** 操作人姓名（写入日志，可空） */
    private String operatorName;
    /** 操作人类型（admin/system/channel 等，可空默认 system） */
    private String operatorType;
}
