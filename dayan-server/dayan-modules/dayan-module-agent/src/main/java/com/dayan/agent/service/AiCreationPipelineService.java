package com.dayan.agent.service;

import com.dayan.agent.dto.AiOutlineConfirmDTO;
import com.dayan.agent.dto.AiOutlineRegenDTO;
import com.dayan.agent.dto.AiReviseDTO;
import com.dayan.agent.dto.AiStrategyConfirmDTO;
import com.dayan.agent.dto.AiTitleRegenDTO;
import com.dayan.agent.vo.AiProjectVO;

/** AI 创作六阶段流水线编排（各阶段一次 LLM 调用，产物落 ai_creation_project） */
public interface AiCreationPipelineService {
    /** 素材消化：素材 → 硬数据清单（factDigest），CREATED→DIGESTED，可重跑 */
    AiProjectVO digest(Long id);
    /** 策略+5标题（factDigest 为空自动先消化；重入清空下游产物） */
    AiProjectVO strategy(Long id);
    /** 带反馈重出标题（策略锁定，重入清空下游产物） */
    AiProjectVO regenerateTitles(Long id, AiTitleRegenDTO dto);
    /** 锁定策略+选定标题 → STRATEGY_CONFIRMED */
    AiProjectVO confirmStrategy(Long id, AiStrategyConfirmDTO dto);
    /** 生成大纲（contentType≠2；重入清空 body 侧产物） */
    AiProjectVO outline(Long id);
    /** 带反馈重生成大纲 */
    AiProjectVO regenerateOutline(Long id, AiOutlineRegenDTO dto);
    /** 锁定大纲（可含前端微调）→ OUTLINE_CONFIRMED */
    AiProjectVO confirmOutline(Long id, AiOutlineConfirmDTO dto);
    /** 正文流水（SSE 可听）：body→audit→polish，产物落库 → BODY_DONE；返回终态 VO */
    AiProjectVO bodyStream(Long id, AiGenerateProgressListener listener);
    /** 段落勘误（最小化修订，不改文风） */
    AiProjectVO revise(Long id, AiReviseDTO dto);
}
