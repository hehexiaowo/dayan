package com.dayan.agent.service;

import com.dayan.agent.dto.AgentShareRecordCreateDTO;
import com.dayan.agent.dto.AgentShareRecordQueryDTO;
import com.dayan.agent.vo.AgentShareRecordVO;
import com.dayan.common.core.resp.PageResult;

import java.util.List;

/**
 * 代理人分享记录（agent_share_record）服务。
 */
public interface AgentShareRecordService {

    PageResult<AgentShareRecordVO> page(AgentShareRecordQueryDTO query);

    /** 新增分享记录，返回 share_code */
    String create(AgentShareRecordCreateDTO dto);

    /** 查代理人分享记录列表 */
    List<AgentShareRecordVO> listByAgent(String agentCode);
}
