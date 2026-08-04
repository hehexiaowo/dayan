package com.dayan.supplier.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.dayan.common.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;
import java.time.LocalDate;
/**
 * 表 supplier_info 对应实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("supplier_info")
public class SupplierInfo extends BaseEntity {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;


    /** 供应商编码 */
    private String supplierCode;

    /** 供应商全称 */
    private String fullName;

    /** 简称 */
    private String shortName;

    /** 供应商类型 */
    private Integer supplierType;

    /** 统一社会信用代码 */
    private String unifiedCreditCode;

    /** 法定代表人 */
    private String legalPerson;

    /** 注册资本(万元) */
    private BigDecimal registeredCapital;

    /** 成立日期 */
    private LocalDate establishDate;

    /** 营业执照编号 */
    private String businessLicenseNo;

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

    /** 供应商介绍 */
    private String description;

    /** 营业执照图片URL */
    private String licenseImage;

    /** 资质证书图片URL */
    private String qualificationImage;

    /** 开户银行 */
    private String bankName;

    /** 银行账号 */
    private String bankAccount;

    /** 银行户名 */
    private String bankAccountName;

    /** 下属机构数量 */
    private Integer parkCount;

    /** 合作开始日期 */
    private LocalDate cooperationStartDate;

    /** 合作结束日期 */
    private LocalDate cooperationEndDate;

    /** 默认佣金比例 */
    private BigDecimal commissionRate;

    /** 状态 */
    private Integer status;

    /** 审核状态 */
    private Integer auditStatus;

    /** 审核备注 */
    private String auditRemark;

    /** 排序号 */
    private Integer sortOrder;

    /** 备注 */
    private String remark;
}
