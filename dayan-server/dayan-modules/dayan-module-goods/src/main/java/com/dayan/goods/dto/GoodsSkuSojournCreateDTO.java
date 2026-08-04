package com.dayan.goods.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 旅居 SKU（goods_sku_sojourn）创建入参。
 *
 * <p>{@code skuCode} 由系统生成（GJ 前缀 + 5 位序列）；{@code parkCode}/{@code roomTypeCode} 弱校验。
 */
@Data
public class GoodsSkuSojournCreateDTO {

    @NotBlank(message = "商品编码不能为空")
    private String goodsCode;

    @Size(max = 200)
    private String skuName;

    /** 机构编码（弱校验） */
    @NotBlank(message = "机构编码不能为空")
    @Size(max = 50)
    private String parkCode;

    /** 关联房间类型编码（弱校验） */
    @NotBlank(message = "房间类型编码不能为空")
    @Size(max = 50)
    private String roomTypeCode;

    private String roomTypeName;
    private String careTypeCode;
    private String foodTypeCode;
    private BigDecimal skuPrice;
    private String priceUnit;
    private Integer minDays;
    private Integer maxDays;
    private Integer stock;
    private LocalDate effectiveDate;
    private LocalDate expireDate;
    private Integer sortOrder;
    private Integer status;
}
