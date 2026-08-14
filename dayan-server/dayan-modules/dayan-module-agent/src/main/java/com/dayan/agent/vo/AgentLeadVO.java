package com.dayan.agent.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 代理人线索 VO（Agent 端）。
 *
 * <p>注意：id 为雪花 ID（19 位），超过 JS 安全整数范围（2^53-1），
 * 使用 {@link ToStringSerializer} 序列化为字符串，防止前端精度丢失。
 */
@Data
public class AgentLeadVO {

    @JsonSerialize(using = ToStringSerializer.class)
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

    /** 关联访客线索编码（lead_info.lead_code，线索池认领后回填） */
    private String visitorLeadCode;

    /** 访客令牌 */
    private String visitorToken;
    /** 访客来源（wechat/browser/unknown） */
    private String visitorSource;
    /** 微信昵称 */
    private String wxNickname;
    /** 微信头像URL */
    private String wxAvatar;
    /** 最后互动时间 */
    private LocalDateTime lastTraceTime;
    /** 最后互动类型（1=内容 2=工具 3=海报） */
    private Integer lastTraceType;
    /** 互动总次数 */
    private Integer traceCount;

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
