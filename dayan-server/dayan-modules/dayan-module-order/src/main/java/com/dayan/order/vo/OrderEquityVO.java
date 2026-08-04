package com.dayan.order.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 权益采购订单（order_equity）VO。
 */
@Data
public class OrderEquityVO {

    private Long id;
    private String orderCode;
    /** 采购来源：1=对公 / 2=个人 */
    private Integer orderSource;
    private String channelCode;
    private String channelFullName;
    private String agentCode;
    private String agentFullName;
    private String distributorCode;
    private String distributorFullName;
    private String goodsCode;
    private String goodsName;
    private String skuCode;
    private String skuName;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal totalAmount;
    private BigDecimal discountAmount;
    private BigDecimal payAmount;
    /** 支付方式：1=微信 / 2=支付宝 / 3=银行转账 / 4=余额 / 5=线下 */
    private Integer payType;
    private LocalDateTime payTime;
    private String payTradeNo;
    /** 权益入库方式：1=批量 / 2=逐张 / 3=自动入库 */
    private Integer deliverType;
    private Integer deliverCount;
    private LocalDateTime deliverTime;
    private LocalDateTime expireTime;
    private Integer invoiceStatus;
    private String organCode;
    /** 订单状态：0=待支付 / 1=已支付 / 2=部分发放 / 3=已发放 / 4=已完成 / 5=已取消 / 6=退款中 / 7=已退款 */
    private Integer orderStatus;
    private String cancelReason;
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
