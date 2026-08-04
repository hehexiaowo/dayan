package com.dayan.park.dto;

import lombok.Data;

/**
 * 机构 VR 查询入参。
 */
@Data
public class ParkMediaVrQueryDTO {

    private Long current = 1L;
    private Long size = 20L;
    private String parkCode;
    private Integer vrType;
    private Integer status;
}
