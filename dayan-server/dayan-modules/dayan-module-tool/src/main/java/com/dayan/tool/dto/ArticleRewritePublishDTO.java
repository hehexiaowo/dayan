package com.dayan.tool.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** 发布文章 */
@Data
public class ArticleRewritePublishDTO {

    /** 发布渠道（wechat=公众号 / xhs=小红书） */
    @NotBlank(message = "请选择发布渠道")
    private String channel;
}
