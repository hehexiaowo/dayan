package com.dayan.park.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 机构设施更新入参。
 */
@Data
public class ParkFacilityUpdateDTO {

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
}
