package com.dayan.service.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDate;

/**
 * 回访品控（service_equity_followup）创建入参。
 *
 * <p>followupCode(FU+10) 由服务端生成。4 维满意度（serviceSatisfaction/parkSatisfaction/
 * butlerSatisfaction/overallSatisfaction，1-5），任一 <3 时 isFollowupNeeded=1 +
 * nextFollowupDate 由调用方提供或留空。
 */
@Data
public class ServiceEquityFollowupCreateDTO {

    @NotBlank(message = "会话编码不能为空")
    private String sessionCode;

    /** 关联安排编码（可空） */
    private String arrangeCode;

    @NotBlank(message = "客户编码不能为空")
    private String clientCode;

    private String butlerCode;

    /** 回访类型（1=服务后回访, 2=入住后回访, 3=定期回访, 4=投诉回访） */
    private Integer followupType;

    /** 回访方式（1=电话, 2=微信, 3=上门, 4=问卷） */
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
    private String followupPlan;
    private LocalDate nextFollowupDate;
    private Integer isResolved;
    private String remark;
}
