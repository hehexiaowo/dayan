package com.dayan.order.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 创建旅游短居预订订单（order_sojourn）入参。
 *
 * <p>金额校验：{@code totalAmount = roomFee + careFee + foodFee + otherFee}，
 * {@code payAmount = totalAmount - discountAmount}（旅游短居订单无 couponAmount 字段，仅 discount）。
 * depositAmount 为押金（独立于 totalAmount，可空）。支持权益抵扣 equityCode。
 * 快照字段（channelFullName/parkFullName 等）由 DTO 传入，不强校验存在性。
 */
@Data
public class CreateOrderSojournDTO {

    /** 订单类型（默认 4=旅游短居，可空） */
    private Integer orderType;

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
    /** 商品名称快照（可空，服务端按 goodsCode 查目录覆盖） */
    private String goodsName;
    /** 机构编码 */
    @NotBlank(message = "机构编码不能为空")
    private String parkCode;
    /** 机构名称快照（可空） */
    private String parkFullName;
    /** 房间类型编码（可空） */
    private String roomTypeCode;
    /** SKU编码（可空） */
    private String skuCode;
    /** 规格名称快照（可空，服务端按 skuCode 查目录覆盖） */
    private String skuName;

    /** 入住日期 */
    private LocalDate checkinDate;
    /** 退房日期 */
    private LocalDate checkoutDate;
    /** 入住天数（可空，由 checkin/checkout 自动计算） */
    private Integer stayDays;
    /** 入住人数（可空） */
    private Integer residentCount;
    /** 入住人姓名（可空） */
    private String residentNames;

    /** 照护类型编码（可空） */
    private String careTypeCode;
    /** 餐饮类型编码（可空） */
    private String foodTypeCode;

    /** 房间费用 */
    private BigDecimal roomFee;
    /** 照护费用 */
    private BigDecimal careFee;
    /** 餐饮费用 */
    private BigDecimal foodFee;
    /** 其他费用 */
    private BigDecimal otherFee;

    /** 优惠金额（可空，默认 0） */
    private BigDecimal discountAmount;

    /** 优惠券编码（可空） */
    private String couponCode;

    /** 押金金额（可空，独立于 totalAmount） */
    private BigDecimal depositAmount;

    /** 使用权益编码（可空） */
    private String equityCode;

    /** 联系人姓名（可空） */
    private String contactName;
    /** 联系人电话（可空） */
    private String contactPhone;
    /** 特殊需求（可空） */
    private String specialNeeds;

    /** 备注（可空） */
    private String remark;

    /** 创建人编码（操作人，写入日志 operatorCode，可空默认 system） */
    private String operatorCode;
    /** 创建人姓名（写入日志 operatorName，可空） */
    private String operatorName;
}
