package com.dayan.park.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 餐饮价格（park_food_price）更新入参。
 */
@Data
public class ParkFoodPriceUpdateDTO {

    private Integer priceType;
    private BigDecimal originalPrice;
    private BigDecimal salePrice;
    private BigDecimal discountRate;
    private String priceDescription;
    private LocalDate effectiveDate;
    private LocalDate expireDate;
    private Integer isCurrent;
    private Integer isPromotion;
    private String promotionDescription;
    private Integer sortOrder;
    private Integer status;
}
