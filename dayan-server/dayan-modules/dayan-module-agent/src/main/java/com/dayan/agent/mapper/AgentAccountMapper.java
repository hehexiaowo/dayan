package com.dayan.agent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dayan.agent.entity.AgentAccount;
import org.apache.ibatis.annotations.Mapper;

/**
 * agent_account 数据访问层。
 */
@Mapper
public interface AgentAccountMapper extends BaseMapper<AgentAccount> {
}
