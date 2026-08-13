package com.dayan.system.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 角色授权树节点（目录 → 菜单 → 操作权限）。
 *
 * <p>nodeKey 全局唯一且带类型前缀，前端 el-tree node-key 直接用：
 * {@code menu:}+{@code menuCode}（目录/菜单）、{@code perm:}+{@code permissionCode}（操作权限）、
 * {@code group:other}（其他权限虚拟组，保存时丢弃）。
 */
@Data
public class MenuGrantTreeVO {

    /** 节点唯一键：'menu:'+menuCode / 'perm:'+permissionCode / 'group:other' */
    private String nodeKey;

    /** 展示名（菜单名/权限名） */
    private String name;

    /** 节点类型：DIR 目录 / MENU 菜单 / PERM 操作权限 / GROUP 虚拟组 */
    private String nodeType;

    private List<MenuGrantTreeVO> children = new ArrayList<>();
}
