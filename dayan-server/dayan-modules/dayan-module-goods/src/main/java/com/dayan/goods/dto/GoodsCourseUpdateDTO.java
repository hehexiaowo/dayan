package com.dayan.goods.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 课程 SKU（goods_course）更新入参。
 */
@Data
public class GoodsCourseUpdateDTO {

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
