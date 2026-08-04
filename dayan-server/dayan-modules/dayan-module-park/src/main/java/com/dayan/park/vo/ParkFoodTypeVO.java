package com.dayan.park.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 餐饮类型 VO。
 */
@Data
public class ParkFoodTypeVO {

    private Long id;
    private String parkCode;
    private String foodTypeCode;
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
    private LocalDateTime createdAt;
}
