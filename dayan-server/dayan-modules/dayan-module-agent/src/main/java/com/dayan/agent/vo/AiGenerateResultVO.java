package com.dayan.agent.vo;

import lombok.Data;

import java.util.List;

/**
 * AI 生成结果 VO（预览用，未落库）。
 */
@Data
public class AiGenerateResultVO {

    private String title;
    private String summary;
    /** 正文（图文=HTML；朋友圈=纯文本；脚本=结构化文本） */
    private String contentBody;
    private Integer contentType;
    /** 生成提示（如知识库未建库/素材缺失等，不阻断） */
    private List<String> warnings;
    /** 备选标题（模型顺带给出，前端可切换） */
    private List<String> alternativeTitles;
    /** 本次写作引用的知识库片段（供代理人核对事实出处） */
    private List<AiMaterialSourceVO> sources;
}
