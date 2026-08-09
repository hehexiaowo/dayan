package com.dayan.goods.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 旅居 SKU（goods_sojourn）更新入参。
 */
@Data
public class GoodsSojournUpdateDTO {

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
    private LocalDate effectiveDate;
    private LocalDate expireDate;
    private Integer sortOrder;
    private Integer status;
}
