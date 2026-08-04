package com.dayan.goods.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 旅居 SKU（goods_sku_sojourn）VO。
 */
@Data
public class GoodsSkuSojournVO {

    private Long id;
    private String goodsCode;
    private String skuCode;
    private String skuName;
    private String parkCode;
    private String roomTypeCode;
    private String roomTypeName;
    private String careTypeCode;
    private String foodTypeCode;
    private BigDecimal skuPrice;
    private String priceUnit;
    private Integer minDays;
    private Integer maxDays;
    private Integer stock;
    private Integer salesCount;
    private LocalDate effectiveDate;
    private LocalDate expireDate;
    private Integer sortOrder;
    private Integer status;
    private LocalDateTime createdAt;
}
