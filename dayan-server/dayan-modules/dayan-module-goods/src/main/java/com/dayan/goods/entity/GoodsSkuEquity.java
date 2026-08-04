package com.dayan.goods.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.dayan.common.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;
/**
 * 表 goods_sku_equity 对应实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("goods_sku_equity")
public class GoodsSkuEquity extends BaseEntity {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;


    /** 商品编码 */
    private String goodsCode;

    /** SKU编码 */
    private String skuCode;

    /** SKU名称 */
    private String skuName;

    /** 权益模板编码 */
    private String templateCode;

    /** 权益类型 */
    private Integer equityType;

    /** 权益面值 */
    private BigDecimal equityValue;

    /** SKU售价 */
    private BigDecimal skuPrice;

    /** 库存 */
    private Integer stock;

    /** 已售数量 */
    private Integer salesCount;

    /** 规格描述 */
    private String specDescription;

    /** 排序号 */
    private Integer sortOrder;

    /** 状态 */
    private Integer status;
}
