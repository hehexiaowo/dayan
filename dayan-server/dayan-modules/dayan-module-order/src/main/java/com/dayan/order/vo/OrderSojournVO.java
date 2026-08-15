package com.dayan.order.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 旅游短居预订订单（order_sojourn）VO。
 */
@Data
public class OrderSojournVO {

    private Long id;
    private String orderCode;
    /** 订单类型 */
    private Integer orderType;
    private String channelCode;
    private String channelFullName;
    private String agentCode;
    private String agentFullName;
    private String distributorCode;
    private String distributorFullName;
    private String clientCode;
    private String clientFullName;
    private String goodsCode;
    /** 商品名称(快照) */
    private String goodsName;
    private String parkCode;
    private String parkFullName;
    private String roomTypeCode;
    private String skuCode;
    /** 规格名称(快照) */
    private String skuName;
    private LocalDate checkinDate;
    private LocalDate checkoutDate;
    private Integer stayDays;
    private Integer residentCount;
    private String residentNames;
    private String careTypeCode;
    private String foodTypeCode;
    private BigDecimal roomFee;
    private BigDecimal careFee;
    private BigDecimal foodFee;
    private BigDecimal otherFee;
    private BigDecimal totalAmount;
    private BigDecimal discountAmount;
    private BigDecimal payAmount;
    private String couponCode;
    /** 支付方式：1=微信 / 2=支付宝 / 3=银行转账 / 4=余额 / 5=线下 */
    private Integer payType;
    private LocalDateTime payTime;
    private BigDecimal depositAmount;
    private String equityCode;
    private String contactName;
    private String contactPhone;
    private String specialNeeds;
    /** 订单状态：0=待支付 / 1=已支付 / 2=部分发放 / 3=已发放 / 4=已完成 / 5=已取消 / 6=退款中 / 7=已退款 */
    private Integer orderStatus;
    private String cancelReason;
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
