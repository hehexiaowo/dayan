package com.dayan.agent.service;

import com.dayan.agent.dto.AgentLeadCreateDTO;
import com.dayan.agent.dto.AgentLeadQueryDTO;
import com.dayan.agent.dto.AgentLeadUpdateDTO;
import com.dayan.agent.vo.AgentLeadVO;
import com.dayan.common.core.resp.PageResult;

/**
 * 代理人线索服务。
 */
public interface AgentLeadService {

    /**
     * 分页查询当前代理人的线索。
     */
    PageResult<AgentLeadVO> page(AgentLeadQueryDTO query);

    /**
     * 新增线索（agentCode/channelCode 从登录上下文自动填充）。
     */
    Long create(AgentLeadCreateDTO dto);

    /**
     * 更新线索（含状态变更）。
     */
    void update(Long leadId, AgentLeadUpdateDTO dto);
}
