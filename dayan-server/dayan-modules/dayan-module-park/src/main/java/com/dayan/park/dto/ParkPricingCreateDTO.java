package com.dayan.park.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 机构统一定价新增入参。
 */
@Data
public class ParkPricingCreateDTO {

    @NotBlank(message = "机构编码不能为空")
    private String parkCode;

    private String planName;

    @NotNull(message = "费类不能为空")
    private Integer chargeType;

    private String refType;

    @NotBlank(message = "关联编码不能为空")
    private String refCode;

    private String refName;

    private Integer billingCycle;

    private String priceUnit;

    private BigDecimal originalPrice;

    @NotNull(message = "售价不能为空")
    @DecimalMin(value = "0", message = "售价不能为负")
    private BigDecimal salePrice;

    private BigDecimal discountRate;

    private String priceDescription;

    private String includesItems;

    @NotNull(message = "生效日期不能为空")
    private LocalDate effectiveDate;

    private LocalDate expireDate;

    private Integer isCurrent;

    private Integer isPromotion;

    private String promotionDescription;

    private String priceChangeReason;

    private Integer sortOrder;

    private Integer status;
}
