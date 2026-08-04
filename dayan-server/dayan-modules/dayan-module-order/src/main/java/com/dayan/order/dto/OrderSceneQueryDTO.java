package com.dayan.order.dto;

import lombok.Data;

import java.time.LocalDate;

/**
 * 场景报名订单（order_scene）查询入参（分页 + 多条件）。
 */
@Data
public class OrderSceneQueryDTO {

    private Long current = 1L;
    private Long size = 20L;

    private String orderCode;
    private Integer orderType;
    private String channelCode;
    private String agentCode;
    private String distributorCode;
    private String clientCode;
    private String sceneCode;
    private String scheduleCode;
    private String couponCode;
    private String equityCode;
    private LocalDate activityDateStart;
    private LocalDate activityDateEnd;
    /** 订单状态：0=待支付 / 1=已支付 / 2=部分发放 / 3=已发放 / 4=已完成 / 5=已取消 / 6=退款中 / 7=已退款 */
    private Integer orderStatus;
    private Integer payType;
}
