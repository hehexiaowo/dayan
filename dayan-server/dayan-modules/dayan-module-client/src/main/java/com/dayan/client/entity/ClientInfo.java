package com.dayan.client.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.dayan.common.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
/**
 * 表 client_info 对应实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("client_info")
public class ClientInfo extends BaseEntity {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;


    /** 客户编码 */
    private String clientCode;

    /** 所属渠道编码 */
    private String channelCode;

    /** 客户姓名 */
    private String fullName;

    /** 性别 */
    private Integer gender;

    /** 头像URL */
    private String avatar;

    /** 出生日期 */
    private LocalDate birthday;

    /** 年龄 */
    private Integer age;

    /** 身份证号 */
    private String idCard;

    /** 手机号 */
    private String phone;

    /** 邮箱 */
    private String email;

    /** 省份编码 */
    private String provinceCode;

    /** 城市编码 */
    private String cityCode;

    /** 区划编码 */
    private String districtCode;

    /** 详细地址 */
    private String address;

    /** 国籍 */
    private String nationality;

    /** 民族 */
    private String ethnic;

    /** 学历 */
    private Integer education;

    /** 婚姻状况 */
    private Integer maritalStatus;

    /** 职业 */
    private String profession;

    /** 来源渠道 */
    private Integer sourceType;

    /** 来源代理人编码 */
    private String sourceAgentCode;

    /** 来源渠道编码 */
    private String sourceChannelCode;

    /** 客户等级 */
    private Integer clientLevel;

    /** 持有权益数 */
    private Integer equityCount;

    /** 已使用权益数 */
    private Integer usedEquityCount;

    /** 累计服务次数 */
    private Integer serviceCount;

    /** 累计消费金额 */
    private BigDecimal totalOrderAmount;

    /** 最近服务时间 */
    private LocalDateTime lastServiceTime;

    /** 注册时间 */
    private LocalDateTime registerTime;

    /** 最后登录时间 */
    private LocalDateTime lastLoginTime;

    /** 是否VIP */
    private Integer isVip;

    /** 状态 */
    private Integer status;

    /** 备注 */
    private String remark;
}
