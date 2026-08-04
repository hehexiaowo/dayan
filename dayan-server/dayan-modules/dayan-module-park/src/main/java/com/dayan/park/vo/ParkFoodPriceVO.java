package com.dayan.park.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 餐饮价格 VO。
 */
@Data
public class ParkFoodPriceVO {

    private Long id;
    private String parkCode;
    private String foodTypeCode;
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
    private LocalDateTime createdAt;
}
