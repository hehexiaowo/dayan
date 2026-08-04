package com.dayan.goods.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 商品上下架入参。
 */
@Data
public class GoodsInfoShelfDTO {

    @NotNull(message = "商品编码不能为空")
    private String goodsCode;

    /** 上下架状态：0=下架 1=上架 */
    @NotNull(message = "商品状态不能为空")
    private Integer goodsStatus;
}
