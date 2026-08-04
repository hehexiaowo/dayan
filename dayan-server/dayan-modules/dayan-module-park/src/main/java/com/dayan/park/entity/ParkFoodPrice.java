package com.dayan.park.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.dayan.common.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;
import java.time.LocalDate;
/**
 * 表 park_food_price 对应实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("park_food_price")
public class ParkFoodPrice extends BaseEntity {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;


    /** 机构编码 */
    private String parkCode;

    /** 餐饮类型编码 */
    private String foodTypeCode;

    /** 价格类型 */
    private Integer priceType;

    /** 原价 */
    private BigDecimal originalPrice;

    /** 售价 */
    private BigDecimal salePrice;

    /** 折扣率 */
    private BigDecimal discountRate;

    /** 价格说明 */
    private String priceDescription;

    /** 生效日期 */
    private LocalDate effectiveDate;

    /** 失效日期 */
    private LocalDate expireDate;

    /** 是否当前生效价格 */
    private Integer isCurrent;

    /** 是否促销价 */
    private Integer isPromotion;

    /** 促销说明 */
    private String promotionDescription;

    /** 排序号 */
    private Integer sortOrder;

    /** 状态 */
    private Integer status;
}
