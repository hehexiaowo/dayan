package com.dayan.system.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 系统素材展示对象（后端→前端）。
 */
@Data
public class SystemAssetVO {

    private Long id;
    private String parkCode;
    private Integer assetType;
    /** 存储方式（1=本地OSS 2=外链） */
    private Integer storageType;
    private String assetUrl;
    private String assetName;
    private Integer assetCategory;
    private String description;
    private Long fileSize;

    // 图片专属
    private Integer width;
    private Integer height;
    private Integer isCover;

    // 视频专属
    private String coverUrl;
    private Integer duration;

    // 文件专属
    private String fileFormat;

    // VR 专属
    private String vrProvider;
    private String thumbnailUrl;

    // 来源追踪
    private String sourceType;
    private String sourceRefCode;

    private Integer sortOrder;
    private Integer status;
    private LocalDateTime createdAt;
}
