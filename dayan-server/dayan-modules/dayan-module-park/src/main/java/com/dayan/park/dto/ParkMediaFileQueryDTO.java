package com.dayan.park.dto;

import lombok.Data;

/**
 * 机构文件查询入参。
 */
@Data
public class ParkMediaFileQueryDTO {

    private Long current = 1L;
    private Long size = 20L;
    private String parkCode;
    private Integer fileType;
    private Integer status;
}
