package com.dayan.content.dto;

import lombok.Data;

/**
 * 内容分类更新入参。
 */
@Data
public class ContentCategoryUpdateDTO {

    private String categoryName;
    private String parentCode;
    private Integer categoryType;
    private String icon;
    private String coverImage;
    private String description;
    private Integer contentCount;
    private Integer sortOrder;
    private Integer isVisible;
    private Integer status;
}
