package com.dayan.order.dto;

import lombok.Data;

/**
 * 权益采购订单（order_equity）查询入参（分页 + 多条件）。
 */
@Data
public class OrderEquityQueryDTO {

    private Long current = 1L;
    private Long size = 20L;

    private String orderCode;
    private Integer orderSource;
    private String channelCode;
    private String agentCode;
    private String distributorCode;
    private String goodsCode;
    private String skuCode;
    private String organCode;
    /** 订单状态：0=待支付 / 1=已支付 / 2=部分发放 / 3=已发放 / 4=已完成 / 5=已取消 / 6=退款中 / 7=已退款 */
    private Integer orderStatus;
    private Integer payType;
}
