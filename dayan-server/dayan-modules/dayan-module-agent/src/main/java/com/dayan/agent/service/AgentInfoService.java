package com.dayan.agent.service;

import com.dayan.agent.dto.AgentInfoCreateDTO;
import com.dayan.agent.dto.AgentInfoQueryDTO;
import com.dayan.agent.dto.AgentInfoUpdateDTO;
import com.dayan.agent.vo.AgentInfoVO;
import com.dayan.common.core.resp.PageResult;

/**
 * 代理人信息（agent_info）服务。
 *
 * <p>代理人按渠道隔离：channel_code 由 {@code DayanTenantHandler} 自动追加，
 * 也可由查询入参显式提供。
 */
public interface AgentInfoService {

    /** 分页查询（按 channel_code 过滤） */
    PageResult<AgentInfoVO> page(AgentInfoQueryDTO query);

    /** 代理人详情 */
    AgentInfoVO getDetail(String agentCode);

    /** 新增代理人，返回生成的 agent_code */
    String create(AgentInfoCreateDTO dto);

    /** 修改代理人 */
    void update(String agentCode, AgentInfoUpdateDTO dto);

    /** 删除代理人（逻辑删除） */
    void delete(String agentCode);
}
