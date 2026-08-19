package com.dayan.system.vo;

import lombok.Data;

/** 百炼类目（业务空间级，多级树） */
@Data
public class SystemCategoryVO {
    private String categoryId;
    private String categoryName;
    private String parentCategoryId;
    /** 百炼内置默认类目（不可删除） */
    private Boolean isDefault;
}
