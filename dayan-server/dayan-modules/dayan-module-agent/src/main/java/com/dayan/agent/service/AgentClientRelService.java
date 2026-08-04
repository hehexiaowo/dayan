package com.dayan.agent.service;

import com.dayan.agent.dto.AgentClientRelBindDTO;
import com.dayan.agent.dto.AgentClientRelQueryDTO;
import com.dayan.agent.vo.AgentClientRelVO;
import com.dayan.common.core.resp.PageResult;

import java.util.List;

/**
 * 代理人-客户绑定关系（agent_client_rel）服务。
 */
public interface AgentClientRelService {

    PageResult<AgentClientRelVO> page(AgentClientRelQueryDTO query);

    /** 绑定客户 */
    Long bind(AgentClientRelBindDTO dto);

    /** 解绑 */
    void unbind(Long id);

    /** 查代理人的客户列表 */
    List<AgentClientRelVO> listByAgent(String agentCode);

    /** 查客户的代理人列表 */
    List<AgentClientRelVO> listByClient(String clientCode);
}
