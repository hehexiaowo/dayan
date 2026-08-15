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
}
