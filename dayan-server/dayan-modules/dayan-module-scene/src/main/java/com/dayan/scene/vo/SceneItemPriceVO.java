package com.dayan.scene.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 场景项目定价 VO。
 */
@Data
public class SceneItemPriceVO {

    private Long id;
    private String sceneCode;
    private String sceneItemCode;
    private Integer priceType;
    private BigDecimal originalPrice;
    private BigDecimal salePrice;
    private BigDecimal channelPrice;
    private String priceDescription;
    private LocalDate effectiveDate;
    private LocalDate expireDate;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
