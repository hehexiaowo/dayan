package com.dayan.supplier.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 供应商信息视图对象。
 */
@Data
public class SupplierInfoVO {

    private Long id;
    private String supplierCode;
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
    private Integer parkCount;
    private LocalDate cooperationStartDate;
    private LocalDate cooperationEndDate;
    private BigDecimal commissionRate;
    private Integer status;
    private Integer auditStatus;
    private String auditRemark;
    private Integer sortOrder;
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
