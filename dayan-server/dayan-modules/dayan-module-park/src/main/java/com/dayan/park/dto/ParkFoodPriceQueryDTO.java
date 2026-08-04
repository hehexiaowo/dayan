package com.dayan.park.dto;

import lombok.Data;

/**
 * 餐饮价格（park_food_price）查询入参。
 */
@Data
public class ParkFoodPriceQueryDTO {

    private Long current = 1L;
    private Long size = 20L;
    private String parkCode;
    private String foodTypeCode;
    private Integer priceType;
    private Integer isCurrent;
    private Integer status;
}
