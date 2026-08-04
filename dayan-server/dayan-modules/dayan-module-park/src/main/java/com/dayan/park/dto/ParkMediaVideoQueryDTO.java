package com.dayan.park.dto;

import lombok.Data;

/**
 * 机构视频查询入参。
 */
@Data
public class ParkMediaVideoQueryDTO {

    private Long current = 1L;
    private Long size = 20L;
    private String parkCode;
    private Integer videoType;
    private Integer status;
}
