package com.dayan.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.dayan.common.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
/**
 * 表 system_dict_common 对应实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("system_dict_common")
public class SystemDictCommon extends BaseEntity {

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

    /** 排序号 */
    private Integer sortOrder;

    /** 图标 */
    private String icon;

    /** 样式类名 */
    private String cssClass;

    /** 状态 */
    private Integer status;

    /** 是否默认项 */
    private Integer isDefault;

    /** 备注 */
    private String remark;
}
