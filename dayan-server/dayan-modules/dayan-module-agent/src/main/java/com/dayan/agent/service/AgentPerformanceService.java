package com.dayan.agent.service;

import com.dayan.agent.dto.AgentPerformanceCreateDTO;
import com.dayan.agent.dto.AgentPerformanceQueryDTO;
import com.dayan.agent.vo.AgentPerformanceSummaryVO;
import com.dayan.agent.vo.AgentPerformanceVO;
import com.dayan.common.core.resp.PageResult;

import java.util.List;

/**
 * 代理人业绩（agent_performance）服务。
 */
public interface AgentPerformanceService {

    PageResult<AgentPerformanceVO> page(AgentPerformanceQueryDTO query);

    /** 新增业绩记录 */
    void create(AgentPerformanceCreateDTO dto);

    /** 查代理人业绩列表 */
    List<AgentPerformanceVO> listByAgent(String agentCode);

    /** 按代理人汇总业绩 */
    AgentPerformanceSummaryVO summary(String agentCode);
}
