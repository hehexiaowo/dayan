package com.dayan.park.dto;

import lombok.Data;

/**
 * 机构顾问查询入参。
 */
@Data
public class ParkAdviserQueryDTO {

    private Long current = 1L;
    private Long size = 20L;
    private String parkCode;
    private String adviserName;
    private Integer isPrimary;
    private Integer status;
}
