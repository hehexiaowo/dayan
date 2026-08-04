package com.dayan.distributor.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 分销商创建入参。
 *
 * <p>{@code distributor_code} 由服务端按 "DS+5 位" 自动生成（{@code CodeGenerator.generate(BusinessCode.DISTRIBUTOR)}）；
 * 必填字段按 {@code subjectType} 差异化校验：
 * <ul>
 *   <li>企业（subjectType=1）：unifiedCreditCode + legalPerson + businessLicenseNo 必填</li>
 *   <li>个人（subjectType=2）：idCard + phone 必填（phone 由 @NotBlank 强制，idCard 由 service 校验）</li>
 * </ul>
 *
 * <p>{@code distributor_info} 为平台共享表，不带 channel_code 隔离条件。
 */
@Data
public class DistributorInfoCreateDTO {

    @NotBlank(message = "分销商全称不能为空")
    @Size(max = 200)
    private String fullName;

    @Size(max = 50)
    private String shortName;

    /** 主体类型（1=企业, 2=个人） */
    @NotNull(message = "主体类型不能为空")
    private Integer subjectType;

    /** 统一社会信用代码（企业必填） */
    @Size(max = 50)
    private String unifiedCreditCode;

    /** 法定代表人（企业必填） */
    @Size(max = 50)
    private String legalPerson;

    /** 营业执照号（企业必填） */
    @Size(max = 100)
    private String businessLicenseNo;

    /** 注册资本（企业） */
    private BigDecimal registeredCapital;

    /** 成立日期（企业） */
    private LocalDate establishDate;

    /** 身份证号（个人必填） */
    @Size(max = 20)
    private String idCard;

    /** 性别（个人，0=未知, 1=男, 2=女） */
    private Integer gender;

    /** 联系电话（个人必填，企业也建议填写） */
    @NotBlank(message = "联系电话不能为空")
    @Size(max = 20)
    private String phone;

    @Size(max = 50)
    private String contactPerson;

    @Size(max = 100)
    private String contactEmail;

    private String provinceCode;
    private String cityCode;
    private String districtCode;

    @Size(max = 500)
    private String address;

    @Size(max = 100)
    private String bankName;

    @Size(max = 50)
    private String bankAccount;

    @Size(max = 100)
    private String bankAccountName;

    /** 状态（0=待审核, 1=已合作, 2=已暂停, 3=已终止） */
    private Integer status;

    /** 排序号 */
    private Integer sortOrder;

    @Size(max = 500)
    private String remark;
}
