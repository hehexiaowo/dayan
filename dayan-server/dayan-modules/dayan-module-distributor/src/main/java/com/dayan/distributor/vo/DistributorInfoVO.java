package com.dayan.distributor.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 分销商信息 VO。
 */
@Data
public class DistributorInfoVO {

    private Long id;
    private String distributorCode;
    private String fullName;
    private String shortName;

    /** 主体类型（1=企业, 2=个人） */
    private Integer subjectType;

    /** 统一社会信用代码（企业） */
    private String unifiedCreditCode;
    /** 法定代表人（企业） */
    private String legalPerson;
    /** 营业执照号（企业） */
    private String businessLicenseNo;
    /** 注册资本（企业） */
    private BigDecimal registeredCapital;
    /** 成立日期（企业） */
    private LocalDate establishDate;

    /** 身份证号（个人） */
    private String idCard;
    /** 性别（个人，0=未知, 1=男, 2=女） */
    private Integer gender;
    /** 联系电话 */
    private String phone;

    private String contactPerson;
    private String contactEmail;

    private String provinceCode;
    private String cityCode;
    private String districtCode;
    private String address;

    private String bankName;
    private String bankAccount;
    private String bankAccountName;

    /** 状态（0=待审核, 1=已合作, 2=已暂停, 3=已终止） */
    private Integer status;
    private Integer sortOrder;
    private String remark;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
