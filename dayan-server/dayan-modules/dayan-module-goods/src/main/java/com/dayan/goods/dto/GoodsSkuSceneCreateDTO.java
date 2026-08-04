package com.dayan.goods.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 场景 SKU（goods_sku_scene）创建入参。
 *
 * <p>{@code skuCode} 由系统生成（GS 前缀 + 5 位序列）；{@code sceneCode} 弱校验。
 */
@Data
public class GoodsSkuSceneCreateDTO {

    @NotBlank(message = "商品编码不能为空")
    private String goodsCode;

    @Size(max = 200)
    private String skuName;

    /** 场景编码（弱校验） */
    @NotBlank(message = "场景编码不能为空")
    @Size(max = 50)
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
