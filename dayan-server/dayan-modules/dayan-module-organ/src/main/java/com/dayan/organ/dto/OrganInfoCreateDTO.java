package com.dayan.organ.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 组织创建入参。
 */
@Data
public class OrganInfoCreateDTO {

    @NotBlank(message = "组织全称不能为空")
    @Size(max = 200)
    private String fullName;

    @Size(max = 50)
    private String shortName;

    /** 1=运营方, 2=子公司, 3=分公司 */
    private Integer organType;

    @Size(max = 50)
    private String unifiedCreditCode;

    private String legalPerson;
    private BigDecimal registeredCapital;
    private LocalDate establishDate;
    private String businessScope;
    private String provinceCode;
    private String cityCode;
    private String districtCode;
    private String address;
    private String contactPerson;
    private String contactPhone;
    private String contactEmail;
    private String logoUrl;
    private String website;
    private String description;
    private Integer sortOrder;
    private String remark;
}
