package com.dayan.service.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 方案定制 VO。
 */
@Data
public class ServiceEquitySolutionVO {

    private Long id;
    private String sessionCode;
    private String demandCode;
    private String clientCode;
    private String butlerCode;
    private String solutionCode;
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
    private LocalDateTime presentationTime;
    private Integer presentationMethod;
    private String clientFeedback;
    private Integer isAccepted;
    private Integer adjustCount;
    private Integer status;
    private String remark;
    private LocalDateTime createdAt;
}
