package com.dayan.goods.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 权益 SKU（goods_sku_equity）更新入参。
 */
@Data
public class GoodsSkuEquityUpdateDTO {

    private String skuName;
    private String templateCode;
    private Integer equityType;
    private BigDecimal equityValue;
    private BigDecimal skuPrice;
    private Integer stock;
    private String specDescription;
    private Integer sortOrder;
    private Integer status;
}
