package com.dayan.park.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 机构统一定价方案 VO。
 */
@Data
public class ParkPricingVO {

    private Long id;
    private String parkCode;
    private String planName;
    private Integer chargeType;
    private String refType;
    private String refCode;
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
    /** 预约生效标记：1=待生效（到点自动切换） */
    private Integer pendingFlag;
    private Integer isPromotion;
    private String promotionDescription;
    private String priceChangeReason;
    private Integer sortOrder;
    private Integer status;
    private Long version;
    private LocalDateTime createdAt;
}
