package com.dayan.park.dto;

import lombok.Data;

/**
 * 机构周边信息查询入参。
 */
@Data
public class ParkPeripheryQueryDTO {

    private Long current = 1L;
    private Long size = 20L;
    private String parkCode;
    private Integer peripheryType;
    private String placeName;
    private Integer status;
}
