package com.dayan.order.vo;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Client 端订单列表项 VO（旅游短居订单 order_sojourn）。
 *
 * <p>字段为客户端「我的订单」展示精简集，含状态文案（后端预计算）。
 */
@Data
@Builder
public class ClientOrderVO {

    private String orderCode;
    private Integer orderStatus;
    /** 状态文案（后端按 OrderEvent 8 态映射） */
    private String statusText;
    /** 标题（= 机构名，兼容前端 Order.title） */
    private String title;
    /** 机构名（快照） */
    private String parkName;
    /** 规格名（房型快照） */
    private String skuName;
    private LocalDate checkinDate;
    private LocalDate checkoutDate;
    private Integer stayDays;
    /** 实付金额 */
    private BigDecimal payAmount;
    /** 订单总额 */
    private BigDecimal totalAmount;
    private LocalDateTime createdAt;
}
