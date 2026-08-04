package com.dayan.content.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 内容分类视图对象。
 */
@Data
public class ContentCategoryVO {

    private Long id;
    private String categoryCode;
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
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
