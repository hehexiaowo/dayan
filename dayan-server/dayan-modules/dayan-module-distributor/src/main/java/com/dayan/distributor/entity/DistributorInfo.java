package com.dayan.distributor.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.dayan.common.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;
import java.time.LocalDate;
/**
 * 表 distributor_info 对应实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("distributor_info")
public class DistributorInfo extends BaseEntity {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;


    /** 分销商编码 */
    private String distributorCode;

    /** 分销商全称 */
    private String fullName;

    /** 简称 */
    private String shortName;

    /** 主体类型 */
    private Integer subjectType;

    /** 统一社会信用代码 */
    private String unifiedCreditCode;

    /** 法定代表人 */
    private String legalPerson;

    /** 营业执照号 */
    private String businessLicenseNo;

    /** 注册资本 */
    private BigDecimal registeredCapital;

    /** 成立日期 */
    private LocalDate establishDate;

    /** 身份证号 */
    private String idCard;

    /** 性别 */
    private Integer gender;

    /** 联系电话 */
    private String phone;

    /** 联系人 */
    private String contactPerson;

    /** 联系邮箱 */
    private String contactEmail;

    /** 省份编码 */
    private String provinceCode;

    /** 城市编码 */
    private String cityCode;

    /** 区划编码 */
    private String districtCode;

    /** 详细地址 */
    private String address;

    /** 开户银行 */
    private String bankName;

    /** 银行账号 */
    private String bankAccount;

    /** 银行户名 */
    private String bankAccountName;

    /** 状态 */
    private Integer status;

    /** 排序号 */
    private Integer sortOrder;

    /** 备注 */
    private String remark;
}
