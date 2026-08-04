package com.dayan.agent.service;

import com.dayan.agent.dto.AgentAccountCreateDTO;
import com.dayan.agent.dto.AgentAccountQueryDTO;
import com.dayan.agent.dto.AgentAccountUpdateDTO;
import com.dayan.agent.vo.AgentAccountVO;
import com.dayan.common.core.resp.PageResult;

/**
 * 代理人账号（agent_account）服务。
 *
 * <p>账号按 channel_code 隔离：查询/更新由 {@code DayanTenantHandler} 自动追加条件，
 * 新增时显式写入 channel_code。
 */
public interface AgentAccountService {

    PageResult<AgentAccountVO> page(AgentAccountQueryDTO query);

    AgentAccountVO getDetail(String agentCode);

    /** 新建账号，返回 agent_code */
    String create(AgentAccountCreateDTO dto);

    void update(String agentCode, AgentAccountUpdateDTO dto);

    /** 重置密码为默认值 */
    void resetPassword(String agentCode);

    void delete(String agentCode);
}
