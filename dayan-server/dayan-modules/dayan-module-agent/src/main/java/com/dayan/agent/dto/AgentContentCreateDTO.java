package com.dayan.agent.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 保存 AI 生成内容（保存 = 快照落库，编辑走 update）。
 */
@Data
public class AgentContentCreateDTO {

    @NotBlank(message = "标题不能为空")
    @Size(max = 200, message = "标题长度不能超过 200")
    private String title;

    /** 摘要（可空） */
    private String summary;

    /** 封面（OSS key，可空） */
    private String coverImage;

    /** 形态（1=图文 2=朋友圈 3=视频脚本） */
    @NotNull(message = "内容形态必选")
    @Min(value = 1, message = "内容形态取值 1-3")
    @Max(value = 3, message = "内容形态取值 1-3")
    private Integer contentType;

    /** 正文（图文=HTML；朋友圈=纯文本；脚本=结构化文本） */
    @NotBlank(message = "正文不能为空")
    private String contentBody;

    /** 风格档位 */
    private String styleCode;

    /** 参考范文 contentCode */
    private String refContentCode;

    /** 勾选知识库文档 JSON */
    private String refKbFiles;

    /** 勾选商品 codes JSON */
    private String refGoodsCodes;

    /** 目标读者（children/elder/general，可空） */
    private String audience;

    /** 文章目的（product/park/science，可空） */
    @Size(max = 32, message = "文章目的非法")
    private String purpose;
}
