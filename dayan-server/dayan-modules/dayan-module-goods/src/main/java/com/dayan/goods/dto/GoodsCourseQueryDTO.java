package com.dayan.goods.dto;

import lombok.Data;

/**
 * 课程 SKU（goods_course）查询入参。
 */
@Data
public class GoodsCourseQueryDTO {

    private Long current = 1L;
    private Long size = 20L;

    private String goodsCode;
    private String skuCode;
    private String skuName;
    private String courseCode;
    private Integer courseType;
    private Integer status;
}
