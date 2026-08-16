package com.dayan.agent.service;

import com.dayan.agent.dto.AiConvertDTO;
import com.dayan.agent.dto.AiTopicsDTO;
import com.dayan.agent.vo.AiGenerateResultVO;

import java.util.List;

/**
 * AI 内容轻量编排：选题灵感 + 形态转换。
 *
 * <p>单次生成链路已由六阶段流水线（{@link AiCreationPipelineService}）取代并下线。
 */
public interface AiContentGenerateService {

    /** 选题灵感：基于勾选素材 + 当前时节出 5 个获客选题方向 */
    List<String> suggestTopics(AiTopicsDTO dto);

    /** 形态转换：已生成内容改写为其他发布形态（事实保持一致，不新增） */
    AiGenerateResultVO convert(AiConvertDTO dto);
}
