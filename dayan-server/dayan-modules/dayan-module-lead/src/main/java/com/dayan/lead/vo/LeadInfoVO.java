package com.dayan.lead.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 访客线索 VO。
 */
@Data
public class LeadInfoVO {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    /** 线索编码（VL 前缀） */
    private String leadCode;
    /** 访客令牌 */
    private String visitorToken;
    /** 所属渠道编码 */
    private String channelCode;
    /** 微信OpenID */
    private String openid;
    /** 手机号（留资后回填） */
    private String phone;
    /** 姓名/称呼 */
    private String name;
    /** 微信昵称 */
    private String wxNickname;
    /** 微信头像URL */
    private String wxAvatar;
    /** 访客环境来源（wechat/browser/unknown） */
    private String visitorSource;
    /** 来源类型（1=内容分享, 2=工具分享, 3=海报分享, 4=直接访问） */
    private Integer sourceType;
    /** 来源编码 */
    private String sourceCode;
    /** 关联客户编码 */
    private String clientCode;
    /** 最后互动时间 */
    private LocalDateTime lastInteractTime;
    /** 最后互动类型（1=内容 2=工具 3=海报） */
    private Integer lastInteractType;
    /** 互动总次数 */
    private Integer interactCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
