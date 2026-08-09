package com.dayan.park.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 机构统一定价更新入参（按字段非空更新）。
 *
 * <p>parkCode / chargeType / refType / refCode 不可变（创建后锁定）。
 */
@Data
public class ParkPricingUpdateDTO {

    private String planName;
    private String refName;
    private Integer billingCycle;
    private String priceUnit;
    private BigDecimal originalPrice;
    private BigDecimal salePrice;
    private BigDecimal discountRate;
    private String priceDescription;
    private String includesItems;
    private LocalDate effectiveDate;
    private LocalDate expireDate;
    private Integer isCurrent;
    private Integer isPromotion;
    private String promotionDescription;
    private String priceChangeReason;
    private Integer sortOrder;
    private Integer status;
}
