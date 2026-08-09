package com.dayan.goods.dto;

import lombok.Data;

/**
 * 场景 SKU（goods_scene）查询入参。
 */
@Data
public class GoodsSceneQueryDTO {

    private Long current = 1L;
    private Long size = 20L;

    private String goodsCode;
    private String skuCode;
    private String skuName;
    private String sceneCode;
    private String parkCode;
    private Integer status;
}
