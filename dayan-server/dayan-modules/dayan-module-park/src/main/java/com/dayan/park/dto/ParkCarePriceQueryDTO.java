package com.dayan.park.dto;

import lombok.Data;

/**
 * 照护价格（park_care_price）查询入参。
 */
@Data
public class ParkCarePriceQueryDTO {

    private Long current = 1L;
    private Long size = 20L;
    private String parkCode;
    private String careTypeCode;
    private Integer priceType;
    private Integer isCurrent;
    private Integer status;
}
