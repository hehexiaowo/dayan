package com.dayan.park.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 机构设施类型 VO。
 */
@Data
public class ParkFacilityTypeVO {

    private Long id;
    private String parkCode;
    private String facilityTypeCode;
    private String facilityTypeName;
    private Integer facilityTypeCategory;
    private String buildingName;
    private String floor;
    private BigDecimal area;
    private Integer capacity;
    private String openTime;
    private String facilityTypeDescription;
    private String coverImage;
    private String images;
    private Integer sortOrder;
    private Integer status;
    private LocalDateTime createdAt;
}
