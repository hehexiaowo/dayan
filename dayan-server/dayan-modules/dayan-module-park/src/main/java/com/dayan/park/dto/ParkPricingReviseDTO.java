package com.dayan.park.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 调价入参（规格 §F.2）：以现记录为基线新建价格版本，不改历史记录。
 * effectiveDate&lt;=今天=立即生效；未来=预约生效（调度器到点翻转）。
 */
@Data
public class ParkPricingReviseDTO {

    @NotNull(message = "新售价不能为空")
    @DecimalMin(value = "0", message = "售价不能为负")
    private BigDecimal salePrice;

    @DecimalMin(value = "0", message = "原价不能为负")
    private BigDecimal originalPrice;

    @DecimalMin(value = "0")
    private BigDecimal discountRate;

    /** 生效日期：&lt;=今天 立即生效；&gt;今天 预约生效 */
    @NotNull(message = "生效日期不能为空")
    private LocalDate effectiveDate;

    @Size(max = 200)
    private String priceChangeReason;
}
