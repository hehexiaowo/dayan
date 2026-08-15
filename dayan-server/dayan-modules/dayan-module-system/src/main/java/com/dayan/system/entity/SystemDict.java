package com.dayan.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.dayan.common.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
/**
 * 表 system_dict 对应实体（原 system_dict_common，54 迁移统一双字典后改名）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("system_dict")
public class SystemDict extends BaseEntity {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;


    /** 字典类型 */
    private String dictType;

    /** 字典编码 */
    private String dictCode;

    /** 字典显示名称 */
    private String dictName;

    /** 字典存储值 */
    private String dictValue;

    /** 父级编码 */
    private String parentCode;

    /** 层级 */
    private Integer level;

    /** 业务域（通用字典为空；业务语义字典标注所属域，如 park/content） */
    private String domain;

    /** 排序号 */
    private Integer sortOrder;

    /** 图标 */
    private String icon;

    /** 样式类名 */
    private String cssClass;

    /** 扩展属性（JSON，如内容分类的 coverImage/isVisible） */
    private String extra;

    /** 状态 */
    private Integer status;

    /** 是否默认项 */
    private Integer isDefault;

    /** 备注 */
    private String remark;
}
