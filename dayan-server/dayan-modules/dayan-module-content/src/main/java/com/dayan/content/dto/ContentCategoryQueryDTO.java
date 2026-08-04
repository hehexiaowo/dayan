package com.dayan.content.dto;

import lombok.Data;

/**
 * 内容分类查询入参。
 */
@Data
public class ContentCategoryQueryDTO {

    private Long current = 1L;
    private Long size = 20L;
    private String categoryCode;
    private String categoryName;
    private String parentCode;
    private Integer categoryType;
    private Integer status;
    private Integer isVisible;
}
