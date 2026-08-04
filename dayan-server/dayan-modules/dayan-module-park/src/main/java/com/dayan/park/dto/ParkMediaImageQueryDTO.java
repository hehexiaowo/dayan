package com.dayan.park.dto;

import lombok.Data;

/**
 * 机构图片查询入参（按 parkCode 维度）。
 */
@Data
public class ParkMediaImageQueryDTO {

    private Long current = 1L;
    private Long size = 20L;
    private String parkCode;
    private Integer imageType;
    private Integer isCover;
    private Integer status;
}
