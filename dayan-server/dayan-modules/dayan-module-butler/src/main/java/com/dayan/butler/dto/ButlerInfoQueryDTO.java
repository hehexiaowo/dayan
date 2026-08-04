package com.dayan.butler.dto;

import lombok.Data;

/**
 * 管家信息查询入参。
 */
@Data
public class ButlerInfoQueryDTO {

    private Long current = 1L;
    private Long size = 20L;
    private String butlerCode;
    private String fullName;
    private String phone;
    private String organCode;
    private Integer butlerLevel;
    private Integer status;
}
