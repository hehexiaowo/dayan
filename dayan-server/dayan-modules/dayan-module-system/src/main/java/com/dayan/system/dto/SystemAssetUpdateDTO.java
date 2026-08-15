package com.dayan.system.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 系统素材更新入参（全字段可选，partial update）。
 */
@Data
public class SystemAssetUpdateDTO {

    /** 类型1：业务维度（park/platform/goods/content/course/scene） */
    @Size(max = 64)
    private String refType1;

    /** 类型2：细分分类（字典 asset_ref_type2） */
    @Size(max = 64)
    private String refType2;

    /** 关联编码：业务实体编码（平台素材为空） */
    @Size(max = 64)
    private String refCode;

    /** 存储方式（1=本地OSS 2=外链） */
    private Integer storageType;

    @Size(max = 500)
    private String assetUrl;

    @Size(max = 200)
    private String assetName;

    @Size(max = 500)
    private String description;

    private Long fileSize;

    // ===== 图片专属 =====
    private Integer width;
    private Integer height;
    private Integer isCover;

    // ===== 视频专属 =====
    @Size(max = 500)
    private String coverUrl;
    private Integer duration;

    // ===== 文件专属 =====
    @Size(max = 20)
    private String fileFormat;

    // ===== VR 专属 =====
    @Size(max = 100)
    private String vrProvider;
    @Size(max = 500)
    private String thumbnailUrl;

    private Integer sortOrder;
    private Integer status;
}
