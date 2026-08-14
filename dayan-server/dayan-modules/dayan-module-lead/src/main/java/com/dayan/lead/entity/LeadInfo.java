package com.dayan.lead.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.dayan.common.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 表 lead_info 对应实体（访客线索唯一身份，归属渠道，不绑定代理人）。
 *
 * <p>身份渐进式补全：visitor_token（匿名）→ openid/union_id（微信授权）→
 * phone/name（留资）→ client_code（注册/建档）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("lead_info")
public class LeadInfo extends BaseEntity {

    /** 主键（雪花ID） */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 线索编码（VL+日期+序号，全平台唯一） */
    private String leadCode;

    /** 访客令牌（匿名唯一标识，UUID） */
    private String visitorToken;

    /** 所属渠道编码（首触渠道） */
    private String channelCode;

    /** 微信OpenID */
    private String openid;

    /** 微信UnionID */
    private String unionId;

    /** 手机号（留资后回填） */
    private String phone;

    /** 姓名/称呼（留资后回填） */
    private String name;

    /** 微信昵称 */
    private String wxNickname;

    /** 微信头像URL */
    private String wxAvatar;

    /** 访客环境来源（wechat/browser/unknown） */
    private String visitorSource;

    /** 来源类型（1=内容分享, 2=工具分享, 3=海报分享, 4=直接访问） */
    private Integer sourceType;

    /** 来源编码（首个触点的 bizCode） */
    private String sourceCode;

    /** 关联客户编码（留资/注册后回填 client_info.client_code） */
    private String clientCode;

    /** 最后互动时间 */
    private LocalDateTime lastInteractTime;

    /** 最后互动类型（1=内容 2=工具 3=海报） */
    private Integer lastInteractType;

    /** 互动总次数 */
    private Integer interactCount;
}
