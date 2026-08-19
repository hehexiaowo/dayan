package com.dayan.channel.vo;

import lombok.Data;

/**
 * Channel 渠道端工作台汇总统计 VO.
 *
 * <p>字段名与前端 {@code DashboardStats} 类型逐字对齐。
 */
@Data
public class DashboardStatsVO {

    /** 本渠道代理人总数 */
    private Long agentCount;

    /** 本渠道客户总数 */
    private Long clientCount;

    /** 本渠道权益总数 */
    private Long equityCount;

    /** 本渠道权益订单总数 */
    private Long orderCount;
}
