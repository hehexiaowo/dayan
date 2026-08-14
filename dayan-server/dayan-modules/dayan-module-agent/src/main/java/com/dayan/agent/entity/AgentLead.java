package com.dayan.agent.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.dayan.common.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 代理人线索（agent_lead）。
 *
 * <p>CRM 潜在客户，非真实客户。转化后关联 client_info（converted_client_code）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("agent_lead")
public class AgentLead extends BaseEntity {

    /** 主键（雪花ID） */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 线索编码（LD+日期+序号，渠道内唯一） */
    private String leadCode;

    /** 归属代理人编码 */
    private String agentCode;

    /** 所属渠道编码 */
    private String channelCode;

    /** 线索姓名（可能只是称呼） */
    private String name;

    /** 联系电话 */
    private String phone;

    /** 性别（0=未知, 1=男, 2=女） */
    private Integer gender;

    /** 年龄 */
    private Integer age;

    /** 线索状态（1=新线索, 2=跟进中, 3=意向, 4=已转化, 5=已流失） */
    private Integer leadStatus;

    /** 来源类型（1=手工录入, 2=分享扫码, 3=活动接触, 4=转介绍, 5=内容引流） */
    private Integer sourceType;

    /** 来源溯源（share_code/activity_code/referrer 等） */
    private String sourceRef;

    /** 关联访客线索编码（lead_info.lead_code，线索池认领后回填） */
    private String visitorLeadCode;

    /** 访客令牌（匿名唯一标识，UUID） */
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

    /** 关注养老类型（旅居/活力长居/照护，逗号分隔） */
    private String interestType;

    /** 关注区域 */
    private String region;

    /** 最后跟进时间 */
    private LocalDateTime lastFollowTime;

    /** 转化后的客户编码（关联 client_info.client_code） */
    private String convertedClientCode;

    /** 转化时间 */
    private LocalDateTime convertedAt;

    /** 备注 */
    private String remark;
}
