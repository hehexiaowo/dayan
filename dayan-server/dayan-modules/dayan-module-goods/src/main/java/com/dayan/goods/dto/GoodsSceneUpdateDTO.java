package com.dayan.goods.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 场景 SKU（goods_scene）更新入参。
 */
@Data
public class GoodsSceneUpdateDTO {

    private String skuName;
    private String sceneCode;
    private String parkCode;
    private BigDecimal skuPrice;
    private Integer personLimit;
    private BigDecimal durationHours;
    private String scheduleDescription;
    private Integer stock;
    private Integer sortOrder;
    private Integer status;
}
