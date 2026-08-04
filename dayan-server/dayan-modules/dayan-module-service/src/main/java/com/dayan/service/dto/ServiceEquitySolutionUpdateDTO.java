package com.dayan.service.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 方案定制（service_equity_solution）更新入参（按 id 更新）。
 */
@Data
public class ServiceEquitySolutionUpdateDTO {

    private String solutionName;
    private Integer solutionType;
    private String recommendedParks;
    private String planSummary;
    private String serviceItems;
    private BigDecimal estimatedCost;
    private String costBreakdown;
    private String timeline;
    private String advantages;
    private String risks;
    private String comparison;
    private Integer presentationMethod;
    private String clientFeedback;
    private Integer isAccepted;
    private Integer status;
    private String remark;
}
