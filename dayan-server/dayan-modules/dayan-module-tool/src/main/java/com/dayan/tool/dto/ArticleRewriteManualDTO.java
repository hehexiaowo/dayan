package com.dayan.tool.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 手动输入文章内容 */
@Data
public class ArticleRewriteManualDTO {

    /** 所属工具实例（tool_info.tool_code，tool_type=aiartist） */
    @NotBlank(message = "工具编码不能为空")
    @Size(max = 50)
    private String toolCode;

    /** 文章标题 */
    @NotBlank(message = "标题不能为空")
    @Size(max = 200, message = "标题长度不能超过200字符")
    private String title;

    /** 文章来源 */
    @Size(max = 100, message = "来源长度不能超过100字符")
    private String source;

    /** 文章正文内容 */
    @NotBlank(message = "内容不能为空")
    @Size(max = 10000, message = "内容长度不能超过10000字符")
    private String content;
}
