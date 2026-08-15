package com.dayan.agent.service;

import com.dayan.agent.dto.AgentContentCreateDTO;
import com.dayan.agent.dto.AgentContentQueryDTO;
import com.dayan.agent.dto.AgentContentUpdateDTO;
import com.dayan.agent.vo.AgentContentVO;
import com.dayan.common.core.resp.PageResult;

/**
 * 代理人 AI 生成个人内容服务（仅本人可见，agentCode 取自登录上下文）。
 */
public interface AgentContentService {

    /** 我的内容分页（按 agentCode + 租户渠道隔离） */
    PageResult<AgentContentVO> page(AgentContentQueryDTO query);

    /** 详情（非本人内容抛 NOT_FOUND） */
    AgentContentVO getDetail(Long id);

    /** 保存生成内容，返回 id */
    Long create(AgentContentCreateDTO dto);

    /** 编辑（null 字段不更新） */
    void update(Long id, AgentContentUpdateDTO dto);

    /** 删除（逻辑删除） */
    void delete(Long id);
}
