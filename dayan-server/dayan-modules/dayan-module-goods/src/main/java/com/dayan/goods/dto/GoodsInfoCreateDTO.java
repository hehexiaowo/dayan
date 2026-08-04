package com.dayan.goods.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品 SPU（goods_info）创建入参。
 *
 * <p>{@code goodsCode} 由系统生成（GD 前缀 + 5 位序列）。
 */
@Data
public class GoodsInfoCreateDTO {

    @NotBlank(message = "商品名称不能为空")
    @Size(max = 200)
    private String goodsName;

    @Size(max = 100)
    private String goodsShortName;

    /** 商品类型：1=权益 2=场景 3=课程 4=旅居 */
    @NotNull(message = "商品类型不能为空")
    private Integer goodsType;

    @Size(max = 50)
    private String categoryCode;

    @Size(max = 100)
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
    private String remark;
}
