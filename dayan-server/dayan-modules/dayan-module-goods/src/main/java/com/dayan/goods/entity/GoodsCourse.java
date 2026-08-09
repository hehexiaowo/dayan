package com.dayan.goods.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.dayan.common.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;
/**
 * 表 goods_course 对应实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("goods_course")
public class GoodsCourse extends BaseEntity {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;


    /** 商品编码 */
    private String goodsCode;

    /** SKU编码 */
    private String skuCode;

    /** SKU名称 */
    private String skuName;

    /** 课程编码 */
    private String courseCode;

    /** 课程类型 */
    private Integer courseType;

    /** SKU售价 */
    private BigDecimal skuPrice;

    /** 课时数 */
    private Integer classCount;

    /** 有效天数 */
    private Integer validDays;

    /** 库存 */
    private Integer stock;

    /** 已售数量 */
    private Integer salesCount;

    /** 排序号 */
    private Integer sortOrder;

    /** 状态 */
    private Integer status;
}
