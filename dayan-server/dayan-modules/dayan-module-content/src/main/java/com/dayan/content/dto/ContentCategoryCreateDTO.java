package com.dayan.content.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 内容分类创建入参。
 */
@Data
public class ContentCategoryCreateDTO {

    @NotBlank(message = "分类名称不能为空")
    @Size(max = 100)
    private String categoryName;

    @Size(max = 50)
    private String categoryCode;

    @Size(max = 50)
    private String parentCode;

    /** 分类类型 */
    private Integer categoryType;

    private String icon;
    private String coverImage;
    private String description;
    private Integer sortOrder;
    private Integer isVisible;
    private Integer status;
}
