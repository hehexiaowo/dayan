package com.dayan.goods.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.dayan.common.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;
import java.time.LocalDate;
/**
 * 表 goods_sojourn 对应实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("goods_sojourn")
public class GoodsSojourn extends BaseEntity {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;


    /** 商品编码 */
    private String goodsCode;

    /** SKU编码 */
    private String skuCode;

    /** SKU名称 */
    private String skuName;

    /** 机构编码 */
    private String parkCode;

    /** 关联房间类型编码 */
    private String roomTypeCode;

    /** 房间类型名称 */
    private String roomTypeName;

    /** 关联照护类型编码 */
    private String careTypeCode;

    /** 关联餐饮类型编码 */
    private String foodTypeCode;

    /** SKU售价 */
    private BigDecimal skuPrice;

    /** 价格单位 */
    private String priceUnit;

    /** 最少天数 */
    private Integer minDays;

    /** 最多天数 */
    private Integer maxDays;

    /** 库存 */
    private Integer stock;

    /** 已售数量 */
    private Integer salesCount;

    /** 生效日期 */
    private LocalDate effectiveDate;

    /** 失效日期 */
    private LocalDate expireDate;

    /** 排序号 */
    private Integer sortOrder;

    /** 状态 */
    private Integer status;
}
