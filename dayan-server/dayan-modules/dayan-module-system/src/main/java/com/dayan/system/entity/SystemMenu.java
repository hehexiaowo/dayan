package com.dayan.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.dayan.common.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 表 system_menu 对应实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("system_menu")
public class SystemMenu extends BaseEntity {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;


    /** 菜单编码 */
    private String menuCode;

    /** 菜单名称 */
    private String menuName;

    /** 父菜单编码 */
    private String parentCode;

    /** 菜单类型 */
    private Integer menuType;

    /** 路由路径 */
    private String path;

    /** 前端组件路径 */
    private String component;

    /** 权限标识 */
    private String permissionCode;

    /** 菜单图标 */
    private String icon;

    /** 排序号 */
    private Integer sortOrder;

    /** 是否可见 */
    private Integer isVisible;

    /** 是否外链 */
    private Integer isExternal;

    /** 是否缓存 */
    private Integer isCache;

    /** 所属域 */
    private String domainType;

    /** 状态 */
    private Integer status;

    /** 备注 */
    private String remark;

    /**
     * 子菜单列表（非数据库字段，仅用于 /menus/tree 接口组装树形结构返回）。
     */
    @TableField(exist = false)
    private List<SystemMenu> children;
}
