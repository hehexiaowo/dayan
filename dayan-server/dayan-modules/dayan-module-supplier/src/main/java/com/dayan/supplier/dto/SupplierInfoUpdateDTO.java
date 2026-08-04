package com.dayan.supplier.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 供应商信息更新入参（{@code supplierCode} 不可改，由路径参数提供）。
 */
@Data
public class SupplierInfoUpdateDTO {

    private String fullName;
    private String shortName;
    private Integer supplierType;
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
