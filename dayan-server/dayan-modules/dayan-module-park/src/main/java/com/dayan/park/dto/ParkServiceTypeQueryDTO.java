package com.dayan.park.dto;

import lombok.Data;

/**
 * 机构服务类型查询入参。
 */
@Data
public class ParkServiceTypeQueryDTO {

    private Long current = 1L;
    private Long size = 20L;
    private String parkCode;
    private String serviceTypeCode;
    private String serviceTypeName;
    private Integer serviceTypeCategory;
    private Integer status;
}
