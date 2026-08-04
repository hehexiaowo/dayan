package com.dayan.goods.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 权益 SKU（goods_sku_equity）创建入参。
 *
 * <p>{@code skuCode} 由系统生成（GE 前缀 + 5 位序列）；{@code templateCode} 弱校验（仅格式非空）。
 */
@Data
public class GoodsSkuEquityCreateDTO {

    @NotBlank(message = "商品编码不能为空")
    private String goodsCode;

    @Size(max = 200)
    private String skuName;

    /** 权益模板编码（弱校验，不跨模块查存在性） */
    @NotBlank(message = "权益模板编码不能为空")
    @Size(max = 50)
    private String templateCode;

    private Integer equityType;
    private BigDecimal equityValue;
    private BigDecimal skuPrice;
    private Integer stock;
    private String specDescription;
    private Integer sortOrder;
    private Integer status;
}
