package com.dayan.organ.dto;

import lombok.Data;

/**
 * 组织查询入参。
 */
@Data
public class OrganInfoQueryDTO {

    private Long current = 1L;
    private Long size = 20L;
    private String organCode;
    private String fullName;
    private Integer organType;
    private Integer status;
}
