package com.dayan.agent.service;

import com.dayan.agent.dto.AiConvertDTO;
import com.dayan.agent.dto.AiGenerateDTO;
import com.dayan.agent.dto.AiTopicsDTO;
import com.dayan.agent.vo.AiGenerateResultVO;

import java.util.List;

/**
 * AI 内容生成编排：聚合范文 + 知识库 RAG 片段 + 商品详情 → 百炼 chat。
 */
public interface AiContentGenerateService {

    /** 生成内容（不落库）。凭据未配置/素材越权时抛 BusinessException。 */
    AiGenerateResultVO generate(AiGenerateDTO dto);

    /** 生成内容（流式：listener 接收阶段与增量回调；行为与非流式一致） */
    AiGenerateResultVO generate(AiGenerateDTO dto, AiGenerateProgressListener listener);

    /** 选题灵感：基于勾选素材 + 当前时节出 5 个获客选题方向 */
    List<String> suggestTopics(AiTopicsDTO dto);

    /** 形态转换：已生成内容改写为其他发布形态（事实保持一致，不新增） */
    AiGenerateResultVO convert(AiConvertDTO dto);
}
