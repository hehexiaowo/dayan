package com.dayan.knowledge.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 知识仓库 VO（含渠道名展示）。
 */
@Data
public class KnowledgeRepoVO {

    private Long id;

    /** 仓库编码（KB+序号） */
    private String repoCode;

    /** 仓库名称 */
    private String repoName;

    /** 归属类型（1=平台大雁养老 2=渠道） */
    private Integer repoType;

    /** 渠道编码 */
    private String channelCode;

    /** 渠道名称（关联查询，失败容错为空） */
    private String channelName;

    /** 渠道简称（关联查询；列表「归属」列展示用，失败容错为空） */
    private String channelShortName;

    /** 百炼远端索引 ID */
    private String indexId;

    /** 建库索引构建任务 ID（构建完成后可为空） */
    private String buildJobId;

    /** 仓库描述 */
    private String description;

    /** 文档数（以百炼远端为准） */
    private Integer docCount;

    /** 状态（0=构建中 1=正常 2=远端异常） */
    private Integer status;

    /** 最近同步时间 */
    private LocalDateTime lastSyncAt;

    /** 排序号 */
    private Integer sortOrder;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
