package com.dayan.park.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 餐饮类型（park_food_type）创建入参。
 *
 * <p>mealPlan：1=三餐 / 2=三餐+点心 / 3=定制。
 */
@Data
public class ParkFoodTypeCreateDTO {

    @NotBlank(message = "机构编码不能为空")
    private String parkCode;

    @NotBlank(message = "餐饮类型编码不能为空")
    @Size(max = 50)
    private String foodTypeCode;

    @NotBlank(message = "餐饮类型名称不能为空")
    @Size(max = 200)
    private String foodTypeName;

    /** 餐饮方案：1=三餐 / 2=三餐+点心 / 3=定制 */
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
