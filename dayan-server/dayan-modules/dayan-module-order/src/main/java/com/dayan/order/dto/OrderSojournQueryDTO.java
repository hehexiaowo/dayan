package com.dayan.order.dto;

import lombok.Data;

import java.time.LocalDate;

/**
 * 旅游短居预订订单（order_sojourn）查询入参（分页 + 多条件）。
 */
@Data
public class OrderSojournQueryDTO {

    private Long current = 1L;
    private Long size = 20L;

    private String orderCode;
    private Integer orderType;
    private String channelCode;
    private String agentCode;
    private String distributorCode;
    private String clientCode;
    private String parkCode;
    private String roomTypeCode;
    private String couponCode;
    private String equityCode;
    private LocalDate checkinDateStart;
    private LocalDate checkinDateEnd;
    /** 订单状态：0=待支付 / 1=已支付 / 2=部分发放 / 3=已发放 / 4=已完成 / 5=已取消 / 6=退款中 / 7=已退款 */
    private Integer orderStatus;
    private Integer payType;
}
