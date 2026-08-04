package com.dayan.organ.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 组织更新入参（organCode 不可改，由路径参数提供）。
 */
@Data
public class OrganInfoUpdateDTO {

    private String fullName;
    private String shortName;
    private Integer organType;
    private String unifiedCreditCode;
    private String legalPerson;
    private BigDecimal registeredCapital;
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
    private Integer status;
    private Integer sortOrder;
    private String remark;
}
