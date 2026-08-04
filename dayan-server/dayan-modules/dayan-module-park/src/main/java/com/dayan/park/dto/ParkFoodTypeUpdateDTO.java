package com.dayan.park.dto;

import lombok.Data;

/**
 * 餐饮类型（park_food_type）更新入参。
 */
@Data
public class ParkFoodTypeUpdateDTO {

    private String foodTypeName;
    private Integer mealPlan;
    private String dietFeatures;
    private String sampleMenu;
    private Integer specialDiet;
    private String specialDietDescription;
    private String description;
    private String coverImage;
    private Integer sortOrder;
    private Integer status;
}
