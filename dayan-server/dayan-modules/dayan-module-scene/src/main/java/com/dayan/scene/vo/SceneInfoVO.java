package com.dayan.scene.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 场景信息 VO。
 */
@Data
public class SceneInfoVO {

    private Long id;
    private String sceneCode;
    private String sceneName;
    private Integer sceneType;
    private String parkCode;
    /** 机构名称 join park_info.full_name */
    private String parkName;
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
    private Integer viewCount;
    private Integer bookCount;
    private Integer sceneStatus;
    private Integer auditStatus;
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
