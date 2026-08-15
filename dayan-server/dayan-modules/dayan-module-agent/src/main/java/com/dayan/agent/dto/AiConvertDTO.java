package com.dayan.agent.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * AI 形态转换请求：已生成内容 → 其他发布形态（一稿多发）。
 */
@Data
public class AiConvertDTO {

    @NotBlank(message = "标题不能为空")
    private String title;

    /** 摘要（可空） */
    private String summary;

    @NotBlank(message = "正文不能为空")
    private String contentBody;

    /** 目标形态（1=图文 2=朋友圈 3=视频脚本） */
    @NotNull(message = "目标形态必选")
    @Min(value = 1, message = "目标形态取值 1-3")
    @Max(value = 3, message = "目标形态取值 1-3")
    private Integer targetContentType;

    /** 风格档位（可空，缺省沿用原稿风格） */
    private String styleCode;
}
