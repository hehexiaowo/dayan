package com.dayan.agent.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 代理人线索 VO（Agent 端）。
 */
@Data
public class AgentLeadVO {

    private Long id;
    private String leadCode;
    private String agentCode;
    private String channelCode;

    private String name;
    private String phone;
    private Integer gender;
    private Integer age;

    /** 线索状态（1=新线索, 2=跟进中, 3=意向, 4=已转化, 5=已流失） */
    private Integer leadStatus;

    /** 来源类型（1=手工录入, 2=分享扫码, 3=活动接触, 4=转介绍, 5=内容引流） */
    private Integer sourceType;
    private String sourceRef;

    /** 意向等级（1=低, 2=中, 3=高） */
    private Integer intentionLevel;
    private String interestType;
    private String region;

    private LocalDateTime lastFollowTime;
    private String convertedClientCode;
    private LocalDateTime convertedAt;
    private String remark;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
