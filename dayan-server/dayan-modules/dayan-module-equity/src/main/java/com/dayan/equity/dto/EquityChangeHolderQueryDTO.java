package com.dayan.equity.dto;

import lombok.Data;

/**
 * 权益更换权益人记录查询入参。
 */
@Data
public class EquityChangeHolderQueryDTO {

    private Long current = 1L;
    private Long size = 20L;

    private String equityCode;
    private Integer changeStatus;
    private String operatorCode;
}
