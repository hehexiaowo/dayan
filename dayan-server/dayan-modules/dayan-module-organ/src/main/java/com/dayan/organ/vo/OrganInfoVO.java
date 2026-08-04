package com.dayan.organ.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 组织信息 VO。
 */
@Data
public class OrganInfoVO {

    private Long id;
    private String organCode;
    private String fullName;
    private String shortName;
    private Integer organType;
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
    private Integer status;
    private Integer sortOrder;
    private String remark;
    private LocalDateTime createdAt;
}
