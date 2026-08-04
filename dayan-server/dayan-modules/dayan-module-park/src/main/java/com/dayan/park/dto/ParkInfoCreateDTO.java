package com.dayan.park.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 机构主信息创建入参。
 *
 * <p>{@code parkCode} 由系统生成（PK+5 位，{@code BusinessCode.PARK}）；
 * {@code operateStatus} 初始为 0（待审核），由状态机流转维护，创建时不允许直接指定。
 */
@Data
public class ParkInfoCreateDTO {

    @NotBlank(message = "机构名称不能为空")
    @Size(max = 200)
    private String fullName;

    @Size(max = 50)
    private String shortName;

    /** 所属供应商编码（须存在且 status=2 已通过） */
    @NotBlank(message = "供应商编码不能为空")
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

    /** 经度（字符串，范围 -180~180） */
    private String longitude;
    /** 纬度（字符串，范围 -90~90） */
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
