package com.dayan.park.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 照护价格（park_care_price）更新入参。
 */
@Data
public class ParkCarePriceUpdateDTO {

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
