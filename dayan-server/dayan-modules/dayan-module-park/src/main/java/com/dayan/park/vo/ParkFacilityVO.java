package com.dayan.park.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 机构设施 VO。
 */
@Data
public class ParkFacilityVO {

    private Long id;
    private String parkCode;
    private String facilityCode;
    private String facilityName;
    private Integer facilityCategory;
    private String buildingName;
    private String floor;
    private BigDecimal area;
    private Integer capacity;
    private String openTime;
    private String facilityDescription;
    private String coverImage;
    private String images;
    private Integer isFree;
    private String feeDescription;
    private Integer sortOrder;
    private Integer status;
    private LocalDateTime createdAt;
}
