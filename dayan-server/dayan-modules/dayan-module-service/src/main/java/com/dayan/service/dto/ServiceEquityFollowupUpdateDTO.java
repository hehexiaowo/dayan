package com.dayan.service.dto;

import lombok.Data;

import java.time.LocalDate;

/**
 * 回访品控（service_equity_followup）更新入参（按 id 更新）。
 */
@Data
public class ServiceEquityFollowupUpdateDTO {

    private Integer followupType;
    private Integer followupMethod;
    private LocalDate followupDate;
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
}
