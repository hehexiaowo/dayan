package com.dayan.goods.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 场景 SKU（goods_sku_scene）VO。
 */
@Data
public class GoodsSkuSceneVO {

    private Long id;
    private String goodsCode;
    private String skuCode;
    private String skuName;
    private String sceneCode;
    private String parkCode;
    private BigDecimal skuPrice;
    private Integer personLimit;
    private BigDecimal durationHours;
    private String scheduleDescription;
    private Integer stock;
    private Integer salesCount;
    private Integer sortOrder;
    private Integer status;
    private LocalDateTime createdAt;
}
