package com.dayan.agent.service;

import com.dayan.agent.dto.AgentLeadCreateDTO;
import com.dayan.agent.dto.AgentLeadQueryDTO;
import com.dayan.agent.dto.AgentLeadUpdateDTO;
import com.dayan.agent.vo.AgentLeadVO;
import com.dayan.common.core.resp.PageResult;
import com.dayan.lead.vo.LeadInfoVO;
import com.dayan.lead.vo.LeadTraceVO;

import java.util.List;

/**
 * 代理人线索服务。
 *
 * <p>agent_lead 为代理人 CRM 线索；访客身份与互动明细归 lead 域（lead_info + 三张互动记录表），
 * 代理人通过「线索池认领」把 lead_info 转为自己的跟进线索（visitor_lead_code 反向关联）。
 */
public interface AgentLeadService {

    /**
     * 分页查询当前代理人的线索。
     */
    PageResult<AgentLeadVO> page(AgentLeadQueryDTO query);

    /**
     * 线索池分页：本渠道内尚未被任何代理人认领的访客线索。
     */
    PageResult<LeadInfoVO> pagePool(String keyword, Boolean onlyWithPhone, long current, long size);

    /**
     * 认领线索池线索：以 lead_info 快照生成当前代理人的 agent_lead。
     *
     * @param visitorLeadCode lead_info.lead_code
     * @return 新线索 ID
     */
    Long claim(String visitorLeadCode);

    /**
     * 新增线索（agentCode/channelCode 从登录上下文自动填充）。
     */
    Long create(AgentLeadCreateDTO dto);

    /**
     * 更新线索（含状态变更）。
     */
    void update(Long leadId, AgentLeadUpdateDTO dto);

    /**
     * 查询单条线索详情（含归属校验）。
     */
    AgentLeadVO detail(Long leadId);

    /**
     * 线索互动时间线（归属校验后从 lead 域三张记录表合并读取，trace_time DESC）。
     */
    List<LeadTraceVO> traces(Long leadId);

    /**
     * 删除线索（软删除）。
     */
    void delete(Long leadId);
}
