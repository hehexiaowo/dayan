package com.dayan.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 系统素材创建入参。
 *
 * storageType 区分本地 OSS 对象（assetUrl=OSS key）与外部链接（assetUrl=完整 http(s) URL）。
 * assetType 区分图片/视频/文件/VR，类型专属字段按需填写。
 * sourceType + sourceRefCode 追踪来源，默认 media_mgmt（素材仓库直录）。
 */
@Data
public class SystemAssetCreateDTO {

    /** 归属机构编码（空=平台素材） */
    @Size(max = 64)
    private String parkCode;

    @NotNull(message = "素材类型不能为空")
    private Integer assetType;

    /** 存储方式（1=本地OSS 2=外链），默认 1 */
    private Integer storageType;

    /** 资源地址：OSS key 或完整外链 URL */
    @NotBlank(message = "资源地址不能为空")
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
