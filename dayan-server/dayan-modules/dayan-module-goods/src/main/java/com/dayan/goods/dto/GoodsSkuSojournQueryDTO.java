package com.dayan.goods.dto;

import lombok.Data;

/**
 * 旅居 SKU（goods_sku_sojourn）查询入参。
 */
@Data
public class GoodsSkuSojournQueryDTO {

    private Long current = 1L;
    private Long size = 20L;

    private String goodsCode;
    private String skuCode;
    private String skuName;
    private String parkCode;
    private String roomTypeCode;
    private Integer status;
}
