package com.dayan.goods.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 课程 SKU（goods_sku_course）VO。
 */
@Data
public class GoodsSkuCourseVO {

    private Long id;
    private String goodsCode;
    private String skuCode;
    private String skuName;
    private String courseCode;
    private Integer courseType;
    private BigDecimal skuPrice;
    private Integer classCount;
    private Integer validDays;
    private Integer stock;
    private Integer salesCount;
    private Integer sortOrder;
    private Integer status;
    private LocalDateTime createdAt;
}
