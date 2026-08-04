package com.dayan.park.dto;

import lombok.Data;

/**
 * 照护类型（park_care_type）查询入参。
 */
@Data
public class ParkCareTypeQueryDTO {

    private Long current = 1L;
    private Long size = 20L;
    private String parkCode;
    private String careTypeCode;
    private String careTypeName;
    private Integer careLevel;
    private Integer status;
}
