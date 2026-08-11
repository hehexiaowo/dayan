package com.dayan.content.vo;

import lombok.Data;

/**
 * 内容分类选项（agent 端分类导航用，仅 code+name 轻量结构）。
 */
@Data
public class ContentCategoryOptionVO {

    private String categoryCode;
    private String categoryName;

    public ContentCategoryOptionVO() {
    }

    public ContentCategoryOptionVO(String categoryCode, String categoryName) {
        this.categoryCode = categoryCode;
        this.categoryName = categoryName;
    }
}
