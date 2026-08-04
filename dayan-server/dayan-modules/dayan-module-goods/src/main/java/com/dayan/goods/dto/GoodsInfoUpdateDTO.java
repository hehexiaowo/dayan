package com.dayan.goods.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品 SPU（goods_info）更新入参。
 *
 * <p>{@code goodsType} 不允许变更（SPU 类型决定关联的 SKU 子表），其余字段均可更新。
 */
@Data
public class GoodsInfoUpdateDTO {

    private String goodsName;
    private String goodsShortName;
    private String categoryCode;
    private String brandName;
    private String coverImage;
    private String imageUrls;
    private String videoUrl;
    private String goodsDescription;
    private String summary;

    private BigDecimal originalPrice;
    private BigDecimal salePrice;
    private BigDecimal costPrice;
    private String priceUnit;
    private Integer stock;
    private LocalDateTime saleStartTime;
    private LocalDateTime saleEndTime;

    private Integer isHot;
    private Integer isNew;
    private Integer isRecommend;
    private Integer sortOrder;
    private Integer auditStatus;
    private String remark;
}
