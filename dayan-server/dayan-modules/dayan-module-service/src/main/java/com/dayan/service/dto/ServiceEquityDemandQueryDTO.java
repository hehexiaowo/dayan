package com.dayan.service.dto;

import lombok.Data;

/**
 * 需求收集（service_equity_demand）查询入参（按 sessionCode 过滤）。
 */
@Data
public class ServiceEquityDemandQueryDTO {

    private Long current = 1L;
    private Long size = 20L;
    private String sessionCode;
    private String demandCode;
    private String clientCode;
    private String butlerCode;
    private Integer demandType;
    private Integer status;
}
