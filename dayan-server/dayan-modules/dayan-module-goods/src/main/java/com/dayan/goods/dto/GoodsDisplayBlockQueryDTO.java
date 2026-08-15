package com.dayan.goods.dto;

import lombok.Data;

/**
 * 商品展示板块查询入参。
 */
@Data
public class GoodsDisplayBlockQueryDTO {

    private Long current = 1L;
    private Long size = 20L;
    private String goodsCode;
    private String blockType;
    private Integer status;
}
