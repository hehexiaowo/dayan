package com.dayan.scene.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 场景信息创建入参。
 *
 * <p>{@code sceneCode} 由后端生成（"SC" + 5 位序列），{@code sceneName} 全表唯一。
 * 关联 {@code parkCode} 弱校验（仅格式校验，不跨模块查存在性）。
 */
@Data
public class SceneInfoCreateDTO {

    @NotBlank(message = "场景名称不能为空")
    private String sceneName;

    /** 场景类型：8 类场景 */
    private Integer sceneType;

    /** 关联养老机构编码（弱校验） */
    private String parkCode;

    private String provinceCode;
    private String cityCode;
    private String districtCode;
    private String address;
    private String sceneDescription;
    private String coverImage;
    private String imageUrls;
    private String videoUrl;
    private Integer capacity;
    private BigDecimal durationHours;
    private String targetAudience;
    private String highlight;
    private String notice;
    private Integer minPerson;
    private Integer maxPerson;
    private BigDecimal originalPrice;
    private BigDecimal salePrice;
    private String priceUnit;
    private Integer isFree;
    private Integer sortOrder;
    private Integer sceneStatus;
    private Integer auditStatus;
    private String remark;
}
