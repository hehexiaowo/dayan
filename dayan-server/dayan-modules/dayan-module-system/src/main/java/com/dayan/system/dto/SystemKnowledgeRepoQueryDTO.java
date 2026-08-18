package com.dayan.system.dto;

import lombok.Data;

/**
 * 知识仓库分页查询 DTO（内嵌分页，对齐 ParkDisplayBlockQueryDTO 模式）。
 */
@Data
public class SystemKnowledgeRepoQueryDTO {

    private long current = 1L;

    private long size = 20L;

    /** 归属类型（1=平台 2=渠道，空=全部） */
    private Integer repoType;

    /** 渠道编码筛选（空=全部） */
    private String channelCode;

    /** 仓库名称模糊筛选 */
    private String repoName;

    /** 状态（0=构建中 1=正常 2=异常） */
    private Integer status;
}
