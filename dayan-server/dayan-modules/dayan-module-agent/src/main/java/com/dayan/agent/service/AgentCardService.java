package com.dayan.agent.service;

import com.dayan.agent.dto.AgentCardCreateDTO;
import com.dayan.agent.dto.AgentCardQueryDTO;
import com.dayan.agent.dto.AgentCardUpdateDTO;
import com.dayan.agent.vo.AgentCardVO;
import com.dayan.common.core.resp.PageResult;

/**
 * 代理人电子名片服务。
 */
public interface AgentCardService {

    /**
     * 分页查询当前代理人的名片列表。
     */
    PageResult<AgentCardVO> page(AgentCardQueryDTO query);

    /**
     * 新增名片（agentCode/channelCode 从登录上下文自动填充）。
     */
    Long create(AgentCardCreateDTO dto);

    /**
     * 更新名片（选择性更新，含归属校验）。
     */
    void update(Long id, AgentCardUpdateDTO dto);

    /**
     * 查询单张名片详情（含归属校验）。
     */
    AgentCardVO detail(Long id);

    /**
     * 删除名片（软删除）。
     */
    void delete(Long id);

    /**
     * 查代理人第一张启用名片（公开分享用，不需登录）。
     *
     * @param agentCode 代理人编码
     * @return 第一张 status=1 的名片，无则 null
     */
    AgentCardVO getFirstByAgent(String agentCode);
}
