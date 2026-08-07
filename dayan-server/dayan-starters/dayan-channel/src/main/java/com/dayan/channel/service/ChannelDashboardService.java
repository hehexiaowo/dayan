package com.dayan.channel.service;

import com.dayan.channel.vo.DashboardStatsVO;

/**
 * Channel 渠道端工作台聚合统计服务。
 *
 * <p>聚合 4 个域（agent/client/equity/order）的本渠道记录数。
 * channelCode 从 {@link com.dayan.common.mybatis.context.ContextHolder} 读取。
 */
public interface ChannelDashboardService {

    /** 当前渠道的 4 项汇总统计 */
    DashboardStatsVO getStats();
}
