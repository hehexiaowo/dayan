package com.dayan.goods.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 商品展示板块 VO。
 */
@Data
public class GoodsDisplayBlockVO {

    private Long id;
    private String goodsCode;
    /** 板块类型 */
    private String blockType;
    /** 板块标题（C端 tab 名） */
    private String blockTitle;
    /** 富文本内容（HTML） */
    private String content;
    /** 图片key列表（JSON数组字符串） */
    private String images;
    /** 图片描述列表（JSON数组字符串） */
    private String imageDescriptions;
    private Integer sortOrder;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
