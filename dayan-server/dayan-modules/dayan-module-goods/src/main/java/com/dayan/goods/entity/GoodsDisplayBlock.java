package com.dayan.goods.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.dayan.common.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 商品展示内容板块实体（C/Agent 端详情页 tab，对齐 park_display_block 模式）。
 *
 * <p>每条 = 一个 tab 的结构化介绍：类型 + 标题 + 富文本 + 多图（带描述）+ 排序 + 状态。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("goods_display_block")
public class GoodsDisplayBlock extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 商品编码（关联goods_info.goods_code） */
    private String goodsCode;

    /** 板块类型（product_intro/rights_detail/service_flow/faq/purchase_terms/custom） */
    private String blockType;

    /** 板块标题（C端 tab 名） */
    private String blockTitle;

    /** 富文本内容（HTML） */
    private String content;

    /** 图片key列表（JSON数组） */
    private String images;

    /** 图片描述列表（JSON数组，与images一一对应） */
    private String imageDescriptions;

    /** 排序号（tab 顺序） */
    private Integer sortOrder;

    /** 状态（0=隐藏, 1=显示） */
    private Integer status;
}
