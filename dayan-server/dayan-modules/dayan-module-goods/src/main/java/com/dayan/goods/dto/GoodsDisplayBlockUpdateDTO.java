package com.dayan.goods.dto;

import lombok.Data;

/**
 * 商品展示板块更新 DTO（全字段可选，null=不修改）。
 */
@Data
public class GoodsDisplayBlockUpdateDTO {

    private String blockType;
    private String blockTitle;
    private String content;
    private String images;
    private String imageDescriptions;
    private Integer sortOrder;
    private Integer status;
}
