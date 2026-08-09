package com.dayan.park.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import com.dayan.common.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 表 park_pricing 对应实体：机构统一定价方案。
 *
 * <p>合并原 park_room_price / park_care_price / park_food_price / park_facility_price / park_service_price。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("park_pricing")
public class ParkPricing extends BaseEntity {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 机构编码 */
    private String parkCode;

    /** 方案名称 */
    private String planName;

    /** 费类（1=房间费 2=照护费 3=餐费 4=押金 5=设施费 6=服务费 9=其他） */
    private Integer chargeType;

    /** 关联类型（room_type/care_type/food_type/facility/service_item/park） */
    private String refType;

    /** 关联编码 */
    private String refCode;

    /** 关联名称（冗余） */
    private String refName;

    /** 计费周期（1=月 2=季 3=半年 4=年 5=一次性） */
    private Integer billingCycle;

    /** 自由文本计费单位（设施/服务的 次/小时/场） */
    private String priceUnit;

    /** 原价 */
    private BigDecimal originalPrice;

    /** 售价 */
    private BigDecimal salePrice;

    /** 折扣率 */
    private BigDecimal discountRate;

    /** 价格说明 */
    private String priceDescription;

    /** 包含项目（JSON数组） */
    private String includesItems;

    /** 生效日期 */
    private LocalDate effectiveDate;

    /** 失效日期（NULL=长期有效） */
    private LocalDate expireDate;

    /** 是否当前生效价格（0=历史 1=当前） */
    private Integer isCurrent;

    /** 是否促销价 */
    private Integer isPromotion;

    /** 促销说明 */
    private String promotionDescription;

    /** 价格变更原因 */
    private String priceChangeReason;

    /** 排序号 */
    private Integer sortOrder;

    /** 状态（0=停用 1=启用） */
    private Integer status;

    /** 乐观锁版本 */
    @Version
    private Long version;
}
