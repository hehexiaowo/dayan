package com.dayan.tool.service;

import com.dayan.tool.dto.AiOutlineConfirmDTO;
import com.dayan.tool.dto.AiOutlineRegenDTO;
import com.dayan.tool.dto.AiReviseDTO;
import com.dayan.tool.dto.AiStrategyConfirmDTO;
import com.dayan.tool.dto.AiTitleRegenDTO;
import com.dayan.tool.vo.ToolAiartistVO;

/** AI 创作六阶段流水线编排（各阶段一次 LLM 调用，产物落 ai_creation_project） */
public interface ToolAiartistPipelineService {
    /** 素材消化：素材 → 硬数据清单（factDigest），CREATED→DIGESTED，可重跑 */
    ToolAiartistVO digest(Long id);
    /** 策略+5标题（factDigest 为空自动先消化；重入清空下游产物） */
    ToolAiartistVO strategy(Long id);
    /** 带反馈重出标题（策略锁定，重入清空下游产物） */
    ToolAiartistVO regenerateTitles(Long id, AiTitleRegenDTO dto);
    /** 锁定策略+选定标题 → STRATEGY_CONFIRMED */
    ToolAiartistVO confirmStrategy(Long id, AiStrategyConfirmDTO dto);
    /** 生成大纲（contentType≠2；重入清空 body 侧产物） */
    ToolAiartistVO outline(Long id);
    /** 带反馈重生成大纲 */
    ToolAiartistVO regenerateOutline(Long id, AiOutlineRegenDTO dto);
    /** 锁定大纲（可含前端微调）→ OUTLINE_CONFIRMED */
    ToolAiartistVO confirmOutline(Long id, AiOutlineConfirmDTO dto);
    /** 正文流水（SSE 可听）：body→audit→polish，产物落库 → BODY_DONE；返回终态 VO */
    ToolAiartistVO bodyStream(Long id, AiGenerateProgressListener listener);
    /** 段落勘误（最小化修订，不改文风） */
    ToolAiartistVO revise(Long id, AiReviseDTO dto);
    /** 逐占位符生成配图（DashScope→MinIO），≥1 成功 → IMAGES_DONE；连续 2 失败降级 prompt 清单 */
    ToolAiartistVO imagesStream(Long id, AiImageProgressListener listener);
    /** 图文 HTML 成品（占位符→图片 URL；未生成/失败的占位符剔除） */
    String previewHtml(Long id);
    /** 保存到内容中心（写 agent_content，含 purpose），→ SAVED，返回内容 id */
    Long saveToContent(Long id);
}
