package com.dayan.order.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 创建场景报名订单（order_scene）入参。
 *
 * <p>金额校验：{@code totalAmount = unitPrice * participantCount}（场景按人头计价），
 * {@code payAmount = totalAmount - discountAmount - couponAmount}。
 * 支持权益抵扣（equityCode）+ 优惠券抵扣（couponCode/couponAmount）。
 * 快照字段（channelFullName/agentFullName 等）由 DTO 传入，不强校验存在性。
 */
@Data
public class CreateOrderSceneDTO {

    /** 渠道编码（可空） */
    private String channelCode;
    /** 渠道名称快照（可空） */
    private String channelFullName;

    /** 代理人编码（可空） */
    private String agentCode;
    /** 代理人姓名快照（可空） */
    private String agentFullName;

    /** 分销商编码（可空） */
    private String distributorCode;
    /** 分销商名称快照（可空） */
    private String distributorFullName;

    /** 客户编码 */
    @NotBlank(message = "客户编码不能为空")
    private String clientCode;
    /** 客户姓名快照（可空） */
    private String clientFullName;

    /** 商品编码（可空） */
    private String goodsCode;
    /** 场景编码 */
    @NotBlank(message = "场景编码不能为空")
    private String sceneCode;
    /** 场景名称 */
    @NotBlank(message = "场景名称不能为空")
    private String sceneName;
    /** SKU编码（可空） */
    private String skuCode;
    /** 排期编码（可空） */
    private String scheduleCode;

    /** 活动日期 */
    private LocalDate activityDate;

    /** 参与人数 */
    @NotNull(message = "参与人数不能为空")
    @Min(value = 1, message = "参与人数必须大于 0")
    private Integer participantCount;

    /** 参与人姓名（JSON 数组字符串，可空） */
    private String participantNames;

    /** 单价（人均） */
    @NotNull(message = "单价不能为空")
    @Min(value = 0, message = "单价不能为负")
    private BigDecimal unitPrice;

    /** 优惠金额（可空，默认 0） */
    private BigDecimal discountAmount;

    /** 优惠券编码（可空） */
    private String couponCode;
    /** 优惠券抵扣金额（可空，默认 0） */
    private BigDecimal couponAmount;

    /** 使用权益编码（可空） */
    private String equityCode;

    /** 联系人姓名（可空） */
    private String contactName;
    /** 联系人电话（可空） */
    private String contactPhone;

    /** 备注（可空） */
    private String remark;

    /** 创建人编码（操作人，写入日志 operatorCode，可空默认 system） */
    private String operatorCode;
    /** 创建人姓名（写入日志 operatorName，可空） */
    private String operatorName;
}
