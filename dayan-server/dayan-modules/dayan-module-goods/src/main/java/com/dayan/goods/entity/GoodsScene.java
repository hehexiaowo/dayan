package com.dayan.goods.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.dayan.common.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;
/**
 * 表 goods_scene 对应实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("goods_scene")
public class GoodsScene extends BaseEntity {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;


    /** 商品编码 */
    private String goodsCode;

    /** SKU编码 */
    private String skuCode;

    /** SKU名称 */
    private String skuName;

    /** 场景编码 */
    private String sceneCode;

    /** 关联机构编码 */
    private String parkCode;

    /** SKU售价 */
    private BigDecimal skuPrice;

    /** 人数限制 */
    private Integer personLimit;

    /** 活动时长(小时) */
    private BigDecimal durationHours;

    /** 排期说明 */
    private String scheduleDescription;

    /** 库存 */
    private Integer stock;

    /** 已售数量 */
    private Integer salesCount;

    /** 排序号 */
    private Integer sortOrder;

    /** 状态 */
    private Integer status;
}
