package com.dayan.goods.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品 SPU（goods_info）更新入参。
 *
 * <p>{@code goodsType} 变更受服务层约束：仅当商品在 4 张 SKU 子表均无配置时允许
 * （类型决定关联的 SKU 子表，已建配置的类型切换会造成孤儿数据），其余字段均可更新。
 */
@Data
public class GoodsInfoUpdateDTO {

    /** 商品类型（1权益/2场景/3课程/4旅游短居）；与存量不同时要求全部 SKU 子表为空 */
    private Integer goodsType;

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

    /** 页面展示配置JSON：{"banners":["key"],"thumbnail":"key"} */
    private String displayConfig;
}
