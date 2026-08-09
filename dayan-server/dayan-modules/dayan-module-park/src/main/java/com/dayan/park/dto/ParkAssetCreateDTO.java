package com.dayan.park.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 机构素材创建入参。
 *
 * assetType 区分图片/视频/文件/VR，类型专属字段按需填写。
 * sourceType + sourceRefCode 追踪来源，默认 media_mgmt（素材库直传）。
 */
@Data
public class ParkAssetCreateDTO {

    @NotBlank(message = "机构编码不能为空")
    private String parkCode;

    @NotNull(message = "素材类型不能为空")
    private Integer assetType;

    @NotBlank(message = "文件key不能为空")
    @Size(max = 500)
    private String assetUrl;

    @Size(max = 200)
    private String assetName;

    /** 业务分类 */
    private Integer assetCategory;

    @Size(max = 500)
    private String description;

    /** 文件大小（字节） */
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

    // ===== 来源追踪 =====
    /** 来源（默认 media_mgmt） */
    private String sourceType;
    /** 来源编码（media_mgmt 时为 NULL） */
    private String sourceRefCode;

    private Integer sortOrder;
    private Integer status;
}
