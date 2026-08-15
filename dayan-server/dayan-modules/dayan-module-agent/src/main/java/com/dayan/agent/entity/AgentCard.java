package com.dayan.agent.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.dayan.common.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 代理人电子名片（agent_card）。
 *
 * <p>一个代理人账号可以创建多张名片（如：面向旅游短居客户 vs 面向长照客户）。
 * 名片用于分享给潜在客户展示代理人信息，可独立于 agent_info 存在。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("agent_card")
public class AgentCard extends BaseEntity {

    /** 主键（雪花ID） */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 名片编码（AC+日期+序号，代理人内唯一） */
    private String cardCode;

    /** 归属代理人编码 */
    private String agentCode;

    /** 所属渠道编码 */
    private String channelCode;

    /** 名片名称（代理人自命名，区分多张名片，如"旅游短居专员名片"） */
    private String cardName;

    /** 显示姓名 */
    private String displayName;

    /** 职务/头衔（如：高级养老规划师） */
    private String title;

    /** 手机号 */
    private String phone;

    /** 微信号 */
    private String wechat;

    /** 邮箱 */
    private String email;

    /** 公司名称 */
    private String company;

    /** 地址 */
    private String address;

    /** 头像（MinIO key 或 http URL） */
    private String avatar;

    /** 个人简介 */
    private String intro;

    /** 专长标签（逗号分隔，如：旅游短居,长照咨询） */
    private String tags;

    /** 排序值（越小越靠前） */
    private Integer sortOrder;

    /** 状态：1=启用 0=停用 */
    private Integer status;
}
