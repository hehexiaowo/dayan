package com.dayan.park.dto;

import lombok.Data;

/**
 * 餐饮类型（park_food_type）查询入参。
 */
@Data
public class ParkFoodTypeQueryDTO {

    private Long current = 1L;
    private Long size = 20L;
    private String parkCode;
    private String foodTypeCode;
    private String foodTypeName;
    private Integer mealPlan;
    private Integer status;
}
