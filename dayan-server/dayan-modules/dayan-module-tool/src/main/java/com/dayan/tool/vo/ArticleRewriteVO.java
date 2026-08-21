package com.dayan.tool.vo;

import lombok.Data;

import java.time.LocalDateTime;

/** 文章转写详情VO */
@Data
public class ArticleRewriteVO {

    private Long id;
    private String toolCode;
    private String agentCode;
    private String channelCode;
    private String status;

    /** 第一步：内容获取结果 */
    private String contentFetch;

    /** 第二步：总结与价值判断 */
    private String summaryAnalysis;

    /** 第三步：转写结果 */
    private String rewriteResult;

    /** 第四步：审核结果 */
    private String auditResult;

    /** 第五步：配图结果 */
    private String imageResult;

    /** 第六步：自查与发布信息 */
    private String publishInfo;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
