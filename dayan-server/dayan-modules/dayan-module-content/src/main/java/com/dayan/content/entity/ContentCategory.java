package com.dayan.content.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.dayan.common.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
/**
 * 表 content_category 对应实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("content_category")
public class ContentCategory extends BaseEntity {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;


    /** 分类编码 */
    private String categoryCode;

    /** 分类名称 */
    private String categoryName;

    /** 父分类编码 */
    private String parentCode;

    /** 分类类型 */
    private Integer categoryType;

    /** 图标 */
    private String icon;

    /** 封面图 */
    private String coverImage;

    /** 分类描述 */
    private String description;

    /** 内容数量 */
    private Integer contentCount;

    /** 排序号 */
    private Integer sortOrder;

    /** 是否可见 */
    private Integer isVisible;

    /** 状态 */
    private Integer status;
}
