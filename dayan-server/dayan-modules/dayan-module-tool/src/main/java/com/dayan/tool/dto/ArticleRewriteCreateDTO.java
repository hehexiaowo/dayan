package com.dayan.tool.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 创建文章转写项目（通过URL链接） */
@Data
public class ArticleRewriteCreateDTO {

    /** 所属工具实例（tool_info.tool_code，tool_type=aiartist） */
    @NotBlank(message = "工具编码不能为空")
    @Size(max = 50)
    private String toolCode;

    /** 文章URL链接 */
    @NotBlank(message = "链接不能为空")
    @Size(max = 500, message = "链接长度不能超过500字符")
    private String url;
}
