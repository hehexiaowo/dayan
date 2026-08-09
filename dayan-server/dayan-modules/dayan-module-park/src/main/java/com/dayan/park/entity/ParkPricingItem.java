package com.dayan.park.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.dayan.common.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 表 park_pricing_item 对应实体：机构定价明细行（套餐关联）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("park_pricing_item")
public class ParkPricingItem extends BaseEntity {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** FK→park_pricing.id */
    private Long pricingId;

    /** 机构编码 */
    private String parkCode;

    /** 关联类型（room_type/care_type/food_type/facility_type/service_type） */
    private String itemType;

    /** 关联编码 */
    private String itemCode;

    /** 关联名称（冗余） */
    private String itemName;

    /** 排序号 */
    private Integer sortOrder;
}
