package com.dayan.agent.service;

import com.dayan.agent.dto.AgentFavoriteAddDTO;
import com.dayan.agent.dto.AgentFavoriteQueryDTO;
import com.dayan.agent.vo.AgentFavoriteVO;
import com.dayan.common.core.resp.PageResult;

import java.util.List;

/**
 * 代理人收藏（agent_favorite）服务。
 */
public interface AgentFavoriteService {

    PageResult<AgentFavoriteVO> page(AgentFavoriteQueryDTO query);

    /** 新增收藏，返回收藏记录 id */
    Long add(AgentFavoriteAddDTO dto);

    /** 取消收藏 */
    void remove(Long id);

    /** 查代理人的收藏列表 */
    List<AgentFavoriteVO> listByAgent(String agentCode);
}
