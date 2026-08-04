package com.dayan.park.dto;

import lombok.Data;

/**
 * 机构设施查询入参。
 */
@Data
public class ParkFacilityQueryDTO {

    private Long current = 1L;
    private Long size = 20L;
    private String parkCode;
    private String facilityCode;
    private String facilityName;
    private Integer facilityCategory;
    private Integer status;
}
