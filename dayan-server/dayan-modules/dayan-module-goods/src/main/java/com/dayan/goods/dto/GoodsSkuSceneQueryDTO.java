package com.dayan.goods.dto;

import lombok.Data;

/**
 * 场景 SKU（goods_sku_scene）查询入参。
 */
@Data
public class GoodsSkuSceneQueryDTO {

    private Long current = 1L;
    private Long size = 20L;

    private String goodsCode;
    private String skuCode;
    private String skuName;
    private String sceneCode;
    private String parkCode;
    private Integer status;
}
