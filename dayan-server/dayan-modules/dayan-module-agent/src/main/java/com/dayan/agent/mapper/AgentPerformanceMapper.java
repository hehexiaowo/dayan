package com.dayan.agent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dayan.agent.entity.AgentPerformance;
import org.apache.ibatis.annotations.Mapper;

/**
 * agent_performance 数据访问层。
 */
@Mapper
public interface AgentPerformanceMapper extends BaseMapper<AgentPerformance> {
}
