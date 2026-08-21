package com.dayan.tool.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 从平台文章引入创建转写项目 */
@Data
public class ArticleRewriteFromArticleDTO {

    /** 所属工具实例（tool_info.tool_code，tool_type=aiartist） */
    @NotBlank(message = "工具编码不能为空")
    @Size(max = 50)
    private String toolCode;

    /** 平台文章ID */
    @NotNull(message = "文章ID不能为空")
    private Long articleId;
}
