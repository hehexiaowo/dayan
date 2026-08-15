package com.dayan.agent.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * AI 生成请求（不落库，预览用）。
 */
@Data
public class AiGenerateDTO {

    /** 形态（1=图文 2=朋友圈 3=视频脚本） */
    @NotNull(message = "内容形态必选")
    @Min(value = 1, message = "内容形态取值 1-3")
    @Max(value = 3, message = "内容形态取值 1-3")
    private Integer contentType;

    /** 风格档位（professional/warm/authoritative/colloquial） */
    @NotBlank(message = "写作风格必选")
    private String styleCode;

    /** 参考范文 contentCode（可空） */
    private String refContentCode;

    /** 勾选知识库文档 fileId（可空） */
    private List<String> kbFileIds;

    /** 勾选商品 goodsCode（可空） */
    private List<String> goodsCodes;

    /** 主题/补充要求（可空，缺省按素材归纳） */
    private String topic;

    /** 目标读者（children=子女决策者/elder=老人本人/general=通用，可空默认 general） */
    private String audience;
}
