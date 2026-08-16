package com.dayan.agent.service;

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
}
