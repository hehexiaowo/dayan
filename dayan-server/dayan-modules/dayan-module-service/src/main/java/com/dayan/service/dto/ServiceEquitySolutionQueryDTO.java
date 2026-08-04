package com.dayan.service.dto;

import lombok.Data;

/**
 * 方案定制（service_equity_solution）查询入参（按 sessionCode 过滤）。
 */
@Data
public class ServiceEquitySolutionQueryDTO {

    private Long current = 1L;
    private Long size = 20L;
    private String sessionCode;
    private String solutionCode;
    private String demandCode;
    private String clientCode;
    private String butlerCode;
    private Integer solutionType;
    private Integer isAccepted;
    private Integer status;
}
