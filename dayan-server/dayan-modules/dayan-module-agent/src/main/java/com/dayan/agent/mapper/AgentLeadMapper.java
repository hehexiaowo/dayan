package com.dayan.agent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dayan.agent.entity.AgentLead;
import org.apache.ibatis.annotations.Mapper;

/**
 * agent_lead 数据访问层。
 */
@Mapper
public interface AgentLeadMapper extends BaseMapper<AgentLead> {
}
