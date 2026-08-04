package com.dayan.supplier.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 供应商信息创建入参。
 *
 * <p>{@code supplierCode} 由系统生成（SP 前缀），{@code status} 初始为 1（待审核）。
 */
@Data
public class SupplierInfoCreateDTO {

    @NotBlank(message = "供应商全称不能为空")
    @Size(max = 200)
    private String fullName;

    @Size(max = 50)
    private String shortName;

    /** 供应商类型：1=养老机构供应商 / 2=商品供应商 / 3=服务供应商 */
    private Integer supplierType;

    @Size(max = 50)
    private String unifiedCreditCode;

    private String legalPerson;
    private BigDecimal registeredCapital;
    private LocalDate establishDate;
    private String businessLicenseNo;
    private String businessScope;
    private String provinceCode;
    private String cityCode;
    private String districtCode;
    private String address;
    private String contactPerson;
    private String contactPhone;
    private String contactEmail;
    private String logoUrl;
    private String description;
    private String licenseImage;
    private String qualificationImage;
    private String bankName;
    private String bankAccount;
    private String bankAccountName;
    private LocalDate cooperationStartDate;
    private LocalDate cooperationEndDate;
    private BigDecimal commissionRate;
    private Integer sortOrder;
    private String remark;
}
