package com.dayan.service.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 服务会话 VO。
 */
@Data
public class ServiceSessionVO {

    private Long id;
    private String sessionCode;
    private String equityCode;
    private String clientCode;
    private String butlerCode;
    private String butlerFullName;
    private Integer serviceType;
    private String serviceTitle;
    private String serviceDescription;
    private Integer priority;
    private Integer sourceType;
    private String sourceCode;
    private String parkCode;
    private String parkFullName;
    private String agentCode;
    private String channelCode;
    private LocalDateTime acceptTime;
    private LocalDateTime completeTime;
    private LocalDateTime closeTime;
    private Integer totalDuration;
    private Integer touchCount;
    private Integer isSatisfied;
    private Integer overallRating;
    private Integer sessionStatus;
    private String subStatus;
    private String closeReason;
    private String remark;
    private LocalDateTime createdAt;
}
