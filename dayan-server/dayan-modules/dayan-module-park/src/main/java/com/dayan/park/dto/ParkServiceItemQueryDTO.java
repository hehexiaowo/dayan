package com.dayan.park.dto;

import lombok.Data;

/**
 * 机构服务项查询入参。
 */
@Data
public class ParkServiceItemQueryDTO {

    private Long current = 1L;
    private Long size = 20L;
    private String parkCode;
    private String serviceCode;
    private String serviceName;
    private Integer serviceCategory;
    private Integer status;
}
