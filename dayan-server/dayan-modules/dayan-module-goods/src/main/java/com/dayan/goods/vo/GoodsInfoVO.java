package com.dayan.goods.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品 SPU（goods_info）VO。
 */
@Data
public class GoodsInfoVO {

    private Long id;
    private String goodsCode;
    private String goodsName;
    private String goodsShortName;
    private Integer goodsType;
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
    private Integer salesCount;
    private Integer viewCount;
    private Integer collectCount;
    private LocalDateTime saleStartTime;
    private LocalDateTime saleEndTime;
    private Integer isHot;
    private Integer isNew;
    private Integer isRecommend;
    private Integer sortOrder;
    private Integer goodsStatus;
    private Integer auditStatus;
    private String remark;
    private String displayConfig;
    private LocalDateTime createdAt;
}
