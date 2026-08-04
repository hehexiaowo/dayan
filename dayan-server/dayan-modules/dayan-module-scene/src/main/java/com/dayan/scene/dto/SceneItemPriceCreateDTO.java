package com.dayan.scene.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 场景项目定价创建入参。
 *
 * <p>按 {@code sceneCode + sceneItemCode} 维度管理项目定价；
 * 渠道差异化定价通过 {@code channelPrice} 字段体现；
 * 批量折扣阶梯由业务侧在调用时按 {@code priceType} 等规则组织，本表保存基础定价。
 */
@Data
public class SceneItemPriceCreateDTO {

    @NotBlank(message = "场景编码不能为空")
    private String sceneCode;

    @NotBlank(message = "场景项目编码不能为空")
    private String sceneItemCode;

    /** 定价类型 */
    private Integer priceType;

    private BigDecimal originalPrice;
    private BigDecimal salePrice;
    /** 渠道专属价（渠道差异化定价维度） */
    private BigDecimal channelPrice;
    private String priceDescription;
    private LocalDate effectiveDate;
    private LocalDate expireDate;
    private Integer status;
}
