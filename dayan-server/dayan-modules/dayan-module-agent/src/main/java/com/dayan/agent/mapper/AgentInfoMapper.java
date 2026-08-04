package com.dayan.agent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dayan.agent.entity.AgentInfo;
import org.apache.ibatis.annotations.Mapper;

/**
 * agent_info 数据访问层。
 */
@Mapper
public interface AgentInfoMapper extends BaseMapper<AgentInfo> {
}
