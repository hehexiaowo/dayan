package com.dayan.park.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 机构设施类型更新入参。
 */
@Data
public class ParkFacilityTypeUpdateDTO {

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
}
