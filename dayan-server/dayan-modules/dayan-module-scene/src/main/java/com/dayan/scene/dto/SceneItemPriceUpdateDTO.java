package com.dayan.scene.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 场景项目定价修改入参（非空字段才更新）。
 */
@Data
public class SceneItemPriceUpdateDTO {

    private Integer priceType;
    private BigDecimal originalPrice;
    private BigDecimal salePrice;
    private BigDecimal channelPrice;
    private String priceDescription;
    private LocalDate effectiveDate;
    private LocalDate expireDate;
    private Integer status;
}
