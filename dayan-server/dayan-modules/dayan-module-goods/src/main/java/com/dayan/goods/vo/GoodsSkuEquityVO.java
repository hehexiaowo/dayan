package com.dayan.goods.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 权益 SKU（goods_sku_equity）VO。
 */
@Data
public class GoodsSkuEquityVO {

    private Long id;
    private String goodsCode;
    private String skuCode;
    private String skuName;
    private String templateCode;
    private Integer equityType;
    private BigDecimal equityValue;
    private BigDecimal skuPrice;
    private Integer stock;
    private Integer salesCount;
    private String specDescription;
    private Integer sortOrder;
    private Integer status;
    private LocalDateTime createdAt;
}
