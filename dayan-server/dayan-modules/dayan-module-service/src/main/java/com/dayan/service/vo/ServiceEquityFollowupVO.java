package com.dayan.service.vo;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 回访品控 VO。
 */
@Data
public class ServiceEquityFollowupVO {

    private Long id;
    private String sessionCode;
    private String arrangeCode;
    private String clientCode;
    private String butlerCode;
    private String followupCode;
    private Integer followupType;
    private Integer followupMethod;
    private LocalDate followupDate;
    private LocalDateTime followupTime;
    private Integer serviceSatisfaction;
    private Integer parkSatisfaction;
    private Integer butlerSatisfaction;
    private Integer overallSatisfaction;
    private String serviceEvaluation;
    private String improvementSuggestions;
    private String complaints;
    private String complaintHandle;
    private Integer isFollowupNeeded;
    private String followupPlan;
    private LocalDate nextFollowupDate;
    private Integer isResolved;
    private Integer status;
    private String remark;
    private LocalDateTime createdAt;
}
