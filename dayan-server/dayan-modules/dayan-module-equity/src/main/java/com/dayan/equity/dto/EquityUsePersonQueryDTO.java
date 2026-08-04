package com.dayan.equity.dto;

import lombok.Data;

/**
 * 权益使用人查询入参。
 */
@Data
public class EquityUsePersonQueryDTO {

    private Long current = 1L;
    private Long size = 20L;

    private String equityCode;
    private String clientCode;
    private String usePersonName;
    private Integer isDefaultHolder;
}
