package com.dayan.organ.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.dayan.common.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;
import java.time.LocalDate;
/**
 * 表 organ_info 对应实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("organ_info")
public class OrganInfo extends BaseEntity {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;


    /** 组织编码 */
    private String organCode;

    /** 组织全称 */
    private String fullName;

    /** 简称 */
    private String shortName;

    /** 组织类型 */
    private Integer organType;

    /** 统一社会信用代码 */
    private String unifiedCreditCode;

    /** 法定代表人 */
    private String legalPerson;

    /** 注册资本(万元) */
    private BigDecimal registeredCapital;

    /** 成立日期 */
    private LocalDate establishDate;

    /** 经营范围 */
    private String businessScope;

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

    /** Logo图片URL */
    private String logoUrl;

    /** 官网地址 */
    private String website;

    /** 组织介绍 */
    private String description;

    /** 营业执照图片URL */
    private String licenseImage;

    /** 状态 */
    private Integer status;

    /** 排序号 */
    private Integer sortOrder;

    /** 备注 */
    private String remark;
}
