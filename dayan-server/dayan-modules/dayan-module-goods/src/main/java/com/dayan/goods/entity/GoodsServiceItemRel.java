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

    /** 排序号 */
    private Integer sortOrder;
}
