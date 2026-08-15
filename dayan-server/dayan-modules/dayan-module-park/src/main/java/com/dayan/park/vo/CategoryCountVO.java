package com.dayan.park.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 机构分类入口数量统计。
 */
@Data
@AllArgsConstructor
public class CategoryCountVO {

    /** 分类标识：vital/care/sojourn */
    private String category;

    /** 分类中文名：活力长居/照护长居/旅游短居 */
    private String categoryName;

    /** 该分类下已发布已上线的机构数 */
    private Integer count;

    /** 是否可用（旅游短居=false，点击提示即将上线） */
    private Boolean available;
}
