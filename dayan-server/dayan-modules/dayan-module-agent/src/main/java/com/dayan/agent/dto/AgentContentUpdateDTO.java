package com.dayan.agent.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 编辑 AI 生成内容（null 字段不更新）。
 */
@Data
public class AgentContentUpdateDTO {

    @Size(max = 200, message = "标题长度不能超过 200")
    private String title;
    private String summary;
    private String coverImage;
    @Min(value = 1, message = "内容形态取值 1-3")
    @Max(value = 3, message = "内容形态取值 1-3")
    private Integer contentType;
    private String contentBody;
    private String styleCode;
}
