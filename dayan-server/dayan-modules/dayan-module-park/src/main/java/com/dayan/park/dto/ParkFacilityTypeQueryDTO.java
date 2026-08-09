package com.dayan.park.dto;

import lombok.Data;

/**
 * 机构设施类型查询入参。
 */
@Data
public class ParkFacilityTypeQueryDTO {

    private Long current = 1L;
    private Long size = 20L;
    private String parkCode;
    private String facilityTypeCode;
    private String facilityTypeName;
    private Integer facilityTypeCategory;
    private Integer status;
}
