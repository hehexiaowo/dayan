package com.dayan.park.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 机构素材更新入参（全字段可选，partial update）。
 */
@Data
public class ParkAssetUpdateDTO {

    @Size(max = 500)
    private String assetUrl;

    @Size(max = 200)
    private String assetName;

    private Integer assetCategory;

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

    // ===== 来源追踪（允许更新来源编码，如新增后补填 code） =====
    private String sourceType;
    private String sourceRefCode;

    private Integer sortOrder;
    private Integer status;
}
