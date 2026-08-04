package com.dayan.park.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 机构主信息更新入参（按字段非空更新）。
 *
 * <p>{@code supplierCode} 允许变更但须通过存在性 + status=2 校验；
 * {@code operateStatus} 不允许直接修改，须走 {@code /transition} 状态机接口。
 */
@Data
public class ParkInfoUpdateDTO {

    private String fullName;
    private String shortName;
    private String supplierCode;
    private String brand;
    private String brandIntroduction;
    private String brandLogo;
    private String operationSubject;
    private String operationSubjectDescription;
    private String importantShareholders;
    private String partnerCompany;
    private String businessLicenseNo;
    private String businessBd;

    private Integer abilityType;
    private String abilityTypeDescription;
    private Integer natureType;
    private String natureTypeDescription;
    private String specialtyTag;
    private Integer dayanLevel;

    private String province;
    private String provinceCode;
    private String city;
    private String cityCode;
    private String district;
    private String districtCode;
    private String address;

    private String longitude;
    private String latitude;

    private String serviceHotline;
    private String baseDescription;
    private String specialtyDescription;
    private String totalArea;
    private String buildingArea;
    private String greenAreaRate;

    private Integer totalBeds;
    private Integer availableBeds;
    private String occupancyRate;
    private Integer staffCount;
    private Integer nurseCount;
    private String nursePatientRatio;

    private Integer minPriceDisplay;
    private Integer maxPriceDisplay;
    private String priceUnit;

    private Integer checkInAgeMin;
    private Integer checkInAgeMax;
    private String checkInDescription;

    private BigDecimal depositAmount;
    private String depositDescription;
    private Integer contractPeriod;

    private Integer sortOrder;
    private Integer isHot;
    private String subScript;
    private String remark;
}
