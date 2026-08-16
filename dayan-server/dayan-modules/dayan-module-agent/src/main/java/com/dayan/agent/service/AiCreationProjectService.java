package com.dayan.agent.service;

import com.dayan.agent.dto.AiProjectCreateDTO;
import com.dayan.agent.entity.AiCreationProject;
import com.dayan.agent.vo.AiProjectListVO;
import com.dayan.agent.vo.AiProjectVO;
import com.dayan.common.core.resp.PageResult;

/** AI 创作项目 CRUD 与状态机守卫（agentCode 强制登录上下文） */
public interface AiCreationProjectService {
    Long create(AiProjectCreateDTO dto);
    AiProjectVO getDetail(Long id);
    PageResult<AiProjectListVO> page(long current, long size, String status);
    void delete(Long id);
    /** 取本人项目（越权按不存在处理），供流水线各阶段复用 */
    AiCreationProject requireOwned(Long id);
    /** 供流水线保存阶段产物（不越权校验，实体来自 requireOwned） */
    int updateById(AiCreationProject entity);
    /** 解析实体为 VO（含素材名回显） */
    AiProjectVO toVO(AiCreationProject entity);
}
