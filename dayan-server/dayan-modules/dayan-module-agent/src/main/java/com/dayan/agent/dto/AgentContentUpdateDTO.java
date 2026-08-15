package com.dayan.agent.dto;

import lombok.Data;

/**
 * 编辑 AI 生成内容（null 字段不更新）。
 */
@Data
public class AgentContentUpdateDTO {

    private String title;
    private String summary;
    private String coverImage;
    private Integer contentType;
    private String contentBody;
    private String styleCode;
}
