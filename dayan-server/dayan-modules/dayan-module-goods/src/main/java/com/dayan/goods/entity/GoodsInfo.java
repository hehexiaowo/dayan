package com.dayan.goods.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.dayan.common.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;
import java.time.LocalDateTime;
/**
 * 表 goods_info 对应实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("goods_info")
public class GoodsInfo extends BaseEntity {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;


    /** 商品编码 */
    private String goodsCode;

    /** 商品名称 */
    private String goodsName;

    /** 商品简称 */
    private String goodsShortName;

    /** 商品类型 */
    private Integer goodsType;

    /** 分类编码 */
    private String categoryCode;

    /** 品牌名称 */
    private String brandName;

    /** 封面图URL */
    private String coverImage;

    /** 商品图片 */
    private String imageUrls;

    /** 宣传视频URL */
    private String videoUrl;

    /** 商品详细描述 */
    private String goodsDescription;

    /** 商品摘要 */
    private String summary;

    /** 原价 */
    private BigDecimal originalPrice;

    /** 售价 */
    private BigDecimal salePrice;

    /** 成本价 */
    private BigDecimal costPrice;

    /** 价格单位 */
    private String priceUnit;

    /** 库存 */
    private Integer stock;

    /** 已售数量 */
    private Integer salesCount;

    /** 浏览次数 */
    private Integer viewCount;

    /** 收藏次数 */
    private Integer collectCount;

    /** 开售时间 */
    private LocalDateTime saleStartTime;

    /** 停售时间 */
    private LocalDateTime saleEndTime;

    /** 是否热销 */
    private Integer isHot;

    /** 是否新品 */
    private Integer isNew;

    /** 是否推荐 */
    private Integer isRecommend;

    /** 排序号 */
    private Integer sortOrder;

    /** 商品状态 */
    private Integer goodsStatus;

    /** 审核状态 */
    private Integer auditStatus;

    /** 备注 */
    private String remark;
}
