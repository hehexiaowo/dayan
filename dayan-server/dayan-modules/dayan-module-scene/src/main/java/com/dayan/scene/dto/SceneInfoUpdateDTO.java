package com.dayan.scene.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 场景信息修改入参（非空字段才更新）。
 */
@Data
public class SceneInfoUpdateDTO {

    private String sceneName;
    private Integer sceneType;
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
