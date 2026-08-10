package com.dayan.park.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 机构主信息 VO。
 */
@Data
public class ParkInfoVO {

    private Long id;
    private String parkCode;
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
    /** 网络标签（vital/care/sojourn 多选） */
    private List<String> networkTags;
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
    private BigDecimal longitude;
    private BigDecimal latitude;

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
    private Integer operateStatus;
    private LocalDateTime openingTime;
    private LocalDateTime onlineTime;
    private LocalDateTime offlineTime;
    private LocalDateTime addPlatformTime;
    private Integer isPublished;
    private Integer viewCount;
    private Integer collectCount;
    private String remark;
    private LocalDateTime createdAt;
}
