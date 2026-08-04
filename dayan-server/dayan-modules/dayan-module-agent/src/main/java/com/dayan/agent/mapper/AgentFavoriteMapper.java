package com.dayan.agent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dayan.agent.entity.AgentFavorite;
import org.apache.ibatis.annotations.Mapper;

/**
 * agent_favorite 数据访问层。
 */
@Mapper
public interface AgentFavoriteMapper extends BaseMapper<AgentFavorite> {
}
