package com.dayan.channel.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.dayan.common.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;
import java.time.LocalDate;
/**
 * 表 channel_info 对应实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("channel_info")
public class ChannelInfo extends BaseEntity {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;


    /** 渠道编码 */
    private String channelCode;

    /** 渠道名称 */
    private String fullName;

    /** 简称 */
    private String shortName;

    /** 渠道类型 */
    private Integer channelType;

    /** 上级渠道编码 */
    private String parentCode;

    /** 祖级列表 */
    private String ancestors;

    /** 层级 */
    private Integer level;

    /** 统一社会信用代码 */
    private String unifiedCreditCode;

    /** 法定代表人 */
    private String legalPerson;

    /** 省份编码 */
    private String provinceCode;

    /** 城市编码 */
    private String cityCode;

    /** 区划编码 */
    private String districtCode;

    /** 详细地址 */
    private String address;

    /** 联系人 */
    private String contactPerson;

    /** 联系电话 */
    private String contactPhone;

    /** 联系邮箱 */
    private String contactEmail;

    /** Logo URL */
    private String logoUrl;

    /** 渠道介绍 */
    private String description;

    /** 旗下代理人数量 */
    private Integer agentCount;

    /** 累计订单金额 */
    private BigDecimal totalOrderAmount;

    /** 合作开始日期 */
    private LocalDate cooperationStartDate;

    /** 分销商编码 */
    private String distributorCode;

    /** 结算周期 */
    private Integer settlementCycle;

    /** 渠道功能开关配置 */
    private String featureConfig;

    /** 排序号 */
    private Integer sortOrder;

    /** 状态 */
    private Integer status;

    /** 审核状态 */
    private Integer auditStatus;

    /** 备注 */
    private String remark;
}
