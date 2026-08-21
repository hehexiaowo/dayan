package com.dayan.tool.vo;

import lombok.Data;

import java.time.LocalDateTime;

/** 文章转写列表项VO */
@Data
public class ArticleRewriteListVO {

    private Long id;
    private String toolCode;
    private String status;

    /** 原文标题（从contentFetch中提取） */
    private String originalTitle;

    /** 转写后的标题（从rewriteResult中提取） */
    private String rewriteTitle;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
