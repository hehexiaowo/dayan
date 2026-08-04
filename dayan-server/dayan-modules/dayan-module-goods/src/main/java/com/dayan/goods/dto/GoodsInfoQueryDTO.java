package com.dayan.goods.dto;

import lombok.Data;

/**
 * 商品 SPU（goods_info）查询入参。
 */
@Data
public class GoodsInfoQueryDTO {

    private Long current = 1L;
    private Long size = 20L;

    private String goodsCode;
    private String goodsName;
    private Integer goodsType;
    private String categoryCode;
    private Integer goodsStatus;
    private Integer auditStatus;
}
