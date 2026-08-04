package com.dayan.goods.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 课程 SKU（goods_sku_course）更新入参。
 */
@Data
public class GoodsSkuCourseUpdateDTO {

    private String skuName;
    private String courseCode;
    private Integer courseType;
    private BigDecimal skuPrice;
    private Integer classCount;
    private Integer validDays;
    private Integer stock;
    private Integer sortOrder;
    private Integer status;
}
