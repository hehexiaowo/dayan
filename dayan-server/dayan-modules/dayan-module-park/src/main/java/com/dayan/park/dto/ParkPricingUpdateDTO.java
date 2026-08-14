package com.dayan.park.dto;

import lombok.Data;

import java.time.LocalDate;

/**
 * 机构统一定价更新入参（按字段非空更新）。
 *
 * <p>parkCode / chargeType / refType / refCode 不可变（创建后锁定）。
 * 价格数值（salePrice/originalPrice/discountRate）与维度（billingCycle/isCurrent）不可直改：调价请走 POST /{id}/revise。
 */
@Data
public class ParkPricingUpdateDTO {

    private String planName;
    private String refName;
    private String priceUnit;
    private String priceDescription;
    private String includesItems;
    private LocalDate effectiveDate;
    private LocalDate expireDate;
    private Integer isPromotion;
    private String promotionDescription;
    private String priceChangeReason;
    private Integer sortOrder;
    private Integer status;
}
