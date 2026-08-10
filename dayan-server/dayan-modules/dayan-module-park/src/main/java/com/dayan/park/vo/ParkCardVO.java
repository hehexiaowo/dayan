package com.dayan.park.vo;

import lombok.Data;

/**
 * 机构列表卡片精简 VO（agent 端用，字段比 ParkInfoVO 少）。
 */
@Data
public class ParkCardVO {

    private String parkCode;
    private String fullName;
    private String shortName;
    private String address;

    // 地理位置信息（地图打点用）
    private String province;
    private String provinceCode;
    private String city;
    private String cityCode;
    private String district;
    private String districtCode;
    private java.math.BigDecimal longitude;
    private java.math.BigDecimal latitude;

    // 床位
    private Integer totalBeds;
    private Integer availableBeds;

    // 价格展示
    private Integer minPriceDisplay;
    private Integer maxPriceDisplay;
    private String priceUnit;

    // 运营状态
    private Integer operateStatus;
    private String abilityTypeDescription;
}
