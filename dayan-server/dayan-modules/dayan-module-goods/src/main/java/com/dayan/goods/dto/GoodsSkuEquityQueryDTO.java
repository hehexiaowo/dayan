package com.dayan.goods.dto;

import lombok.Data;

/**
 * 权益 SKU（goods_sku_equity）查询入参。
 */
@Data
public class GoodsSkuEquityQueryDTO {

    private Long current = 1L;
    private Long size = 20L;

    private String goodsCode;
    private String skuCode;
    private String skuName;
    private String templateCode;
    private Integer equityType;
    private Integer status;
}
