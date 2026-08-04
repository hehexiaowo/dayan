package com.dayan.goods.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 课程 SKU（goods_sku_course）创建入参。
 *
 * <p>{@code skuCode} 由系统生成（GC 前缀 + 5 位序列）；{@code courseCode} 弱校验。
 *
 * <p>说明：规格提及 {@code maxStudents 学员上限}，该约束落地为 {@code stock}（库存即学员上限），
 * 表结构未单独建 maxStudents 字段，沿用 {@code classCount}（课时数）+ {@code validDays}（有效天数）。
 */
@Data
public class GoodsSkuCourseCreateDTO {

    @NotBlank(message = "商品编码不能为空")
    private String goodsCode;

    @Size(max = 200)
    private String skuName;

    /** 课程编码（弱校验） */
    @NotBlank(message = "课程编码不能为空")
    @Size(max = 50)
    private String courseCode;

    private Integer courseType;
    private BigDecimal skuPrice;
    private Integer classCount;
    private Integer validDays;
    /** 库存（学员上限） */
    private Integer stock;
    private Integer sortOrder;
    private Integer status;
}
