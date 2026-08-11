package com.dayan.equity.vo;

import lombok.Data;

/**
 * Agent 端权益卡状态统计 VO。
 *
 * <p>按 equity_status 分组计数，供代理人服务页顶部统计卡片展示。
 */
@Data
public class AgentEquityStatsVO {

    /** 总数（不含已作废） */
    private long total;
    /** 库存中（status=0） */
    private long stock;
    /** 已出库（status=1） */
    private long outbound;
    /** 已激活（status=2） */
    private long activated;
    /** 使用中（status=3） */
    private long inUse;
    /** 已完成（status=4） */
    private long completed;
}
