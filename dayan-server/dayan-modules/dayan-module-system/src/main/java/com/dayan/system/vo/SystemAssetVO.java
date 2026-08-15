package com.dayan.system.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 系统素材展示对象（后端→前端）。
 */
@Data
public class SystemAssetVO {

    private Long id;
    private Integer assetType;
    /** 类型1：业务维度（park/platform/goods/content/course/scene） */
    private String refType1;
    /** 类型2：细分分类（字典 asset_ref_type2） */
    private String refType2;
    /** 关联编码：业务实体编码（平台素材为空） */
    private String refCode;
    /** 存储方式（1=本地OSS 2=外链） */
    private Integer storageType;
    private String assetUrl;
    private String assetName;
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

    private Integer sortOrder;
    private Integer status;
    private LocalDateTime createdAt;
}
