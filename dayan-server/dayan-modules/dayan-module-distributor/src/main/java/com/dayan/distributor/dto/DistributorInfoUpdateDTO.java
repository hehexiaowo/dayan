package com.dayan.distributor.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 分销商更新入参（distributorCode 不可改，由路径参数提供）。
 *
 * <p>所有字段可选，按"非空才更新"语义执行；主体类型 {@code subjectType} 一经确定不允许变更
 * （传值将被忽略并记录日志），相关差异化必填校验仅在 {@code subjectType} 变更时触发。
 */
@Data
public class DistributorInfoUpdateDTO {

    @Size(max = 200)
    private String fullName;

    @Size(max = 50)
    private String shortName;

    /** 主体类型（一般不允许变更，传值将被忽略） */
    private Integer subjectType;

    @Size(max = 50)
    private String unifiedCreditCode;

    @Size(max = 50)
    private String legalPerson;

    @Size(max = 100)
    private String businessLicenseNo;

    private BigDecimal registeredCapital;
    private LocalDate establishDate;

    @Size(max = 20)
    private String idCard;

    private Integer gender;

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

    private Integer status;
    private Integer sortOrder;

    @Size(max = 500)
    private String remark;
}
