package com.dayan.park.dto;

import lombok.Data;

/**
 * 机构展示板块查询入参。
 */
@Data
public class ParkDisplayBlockQueryDTO {

    private Long current = 1L;
    private Long size = 20L;
    private String parkCode;
    private String blockType;
    private Integer status;
}
