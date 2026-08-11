package com.dayan.agent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dayan.agent.entity.AgentCard;
import org.apache.ibatis.annotations.Mapper;

/**
 * agent_card 数据访问层。
 */
@Mapper
public interface AgentCardMapper extends BaseMapper<AgentCard> {
}
