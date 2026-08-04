package com.dayan.park.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.dayan.common.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
/**
 * 表 park_food_type 对应实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("park_food_type")
public class ParkFoodType extends BaseEntity {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;


    /** 机构编码 */
    private String parkCode;

    /** 餐饮类型编码 */
    private String foodTypeCode;

    /** 餐饮类型名称 */
    private String foodTypeName;

    /** 餐饮方案 */
    private Integer mealPlan;

    /** 饮食特色 */
    private String dietFeatures;

    /** 示例菜单 */
    private String sampleMenu;

    /** 是否支持特殊饮食 */
    private Integer specialDiet;

    /** 特殊饮食说明 */
    private String specialDietDescription;

    /** 详细描述 */
    private String description;

    /** 封面图URL */
    private String coverImage;

    /** 排序号 */
    private Integer sortOrder;

    /** 状态 */
    private Integer status;
}
