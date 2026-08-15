package com.dayan.goods.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.dayan.common.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 商品-服务项目关联实体（N:M，一个权益商品组合 N 个服务项目）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("goods_service_item_rel")
public class GoodsServiceItemRel extends BaseEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 商品编码（关联goods_equity.goods_code） */
    private String goodsCode;

    /** 服务项目编码（关联service_item.item_code） */
    private String itemCode;

    /** 数量 */
    private Integer quantity;

    /** 配额周期（1=终身总量,2=年度配额（按激活周年重置）） */
    private Integer quotaType;

    /** 服务网络范围JSON（NetworkScope：NULL=业态全部机构；{mode:custom,parkCodes}=自选） */
    private String networkScope;

    /** 保证入住权（0=无，1=有；长居/照护） */
    private Integer admissionGuaranteed;

    /** 优先入住权（0=无，1=有） */
    private Integer admissionPriority;

    /** 优惠入住权/旅居优惠权（0=无，1=有） */
    private Integer admissionDiscount;

    /** 优惠折扣率（90.00=门市价9折；NULL=按协议未定） */
    private java.math.BigDecimal discountRate;

    /** 单次使用规则JSON（UsageRule：晚数/间数/人数/预订/预定金/取消政策/黑名单） */
    private String usageRule;

    /** 排序号 */
    private Integer sortOrder;
}
