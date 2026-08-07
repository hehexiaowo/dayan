package com.dayan.channel.service.impl;

import com.dayan.agent.entity.AgentInfo;
import com.dayan.agent.mapper.AgentInfoMapper;
import com.dayan.channel.service.ChannelDashboardService;
import com.dayan.channel.vo.DashboardStatsVO;
import com.dayan.client.entity.ClientInfo;
import com.dayan.client.mapper.ClientInfoMapper;
import com.dayan.common.mybatis.context.ContextHolder;
import com.dayan.equity.entity.EquityDepot;
import com.dayan.equity.mapper.EquityDepotMapper;
import com.dayan.order.entity.OrderEquity;
import com.dayan.order.mapper.OrderEquityMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Channel 工作台聚合统计实现。
 *
 * <p>用各域 Mapper 的 selectCount + channelCode 条件直接 count，不引入新的 Service 方法（YAGNI）。
 * 各域 Mapper 由启动类 @MapperScan("com.dayan.**.mapper") 扫描注册。
 */
@Service
@RequiredArgsConstructor
public class ChannelDashboardServiceImpl implements ChannelDashboardService {

    private final AgentInfoMapper agentInfoMapper;
    private final ClientInfoMapper clientInfoMapper;
    private final EquityDepotMapper equityDepotMapper;
    private final OrderEquityMapper orderEquityMapper;

    @Override
    public DashboardStatsVO getStats() {
        String channelCode = ContextHolder.getChannelCode();
        DashboardStatsVO vo = new DashboardStatsVO();
        vo.setAgentCount(agentInfoMapper.selectCount(
                new LambdaQueryWrapper<AgentInfo>().eq(AgentInfo::getChannelCode, channelCode)));
        vo.setClientCount(clientInfoMapper.selectCount(
                new LambdaQueryWrapper<ClientInfo>().eq(ClientInfo::getChannelCode, channelCode)));
        vo.setEquityCount(equityDepotMapper.selectCount(
                new LambdaQueryWrapper<EquityDepot>().eq(EquityDepot::getChannelCode, channelCode)));
        vo.setOrderCount(orderEquityMapper.selectCount(
                new LambdaQueryWrapper<OrderEquity>().eq(OrderEquity::getChannelCode, channelCode)));
        return vo;
    }
}
