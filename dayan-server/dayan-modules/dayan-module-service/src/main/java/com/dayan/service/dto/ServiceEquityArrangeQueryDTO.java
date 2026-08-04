package com.dayan.service.dto;

import lombok.Data;

/**
 * 全程安排（service_equity_arrange）查询入参（按 sessionCode/solutionCode 过滤）。
 */
@Data
public class ServiceEquityArrangeQueryDTO {

    private Long current = 1L;
    private Long size = 20L;
    private String sessionCode;
    private String arrangeCode;
    private String solutionCode;
    private String clientCode;
    private String butlerCode;
    private Integer arrangeType;
    private Integer isConfirmed;
    private Integer status;
}
