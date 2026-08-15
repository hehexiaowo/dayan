package com.dayan.goods.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 商品展示板块创建 DTO。
 */
@Data
public class GoodsDisplayBlockCreateDTO {

    @NotBlank(message = "商品编码不能为空")
    private String goodsCode;

    @NotBlank(message = "板块类型不能为空")
    private String blockType;

    /** 板块标题（C端 tab 名） */
    private String blockTitle;

    /** 富文本内容（HTML） */
    private String content;

    /** 图片key列表（JSON数组字符串） */
    private String images;

    /** 图片描述列表（JSON数组字符串，与images一一对应） */
    private String imageDescriptions;

    private Integer sortOrder;
    private Integer status;
}
