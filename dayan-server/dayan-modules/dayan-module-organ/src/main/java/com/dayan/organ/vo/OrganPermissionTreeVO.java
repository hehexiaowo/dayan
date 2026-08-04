package com.dayan.organ.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 权限树节点 VO。
 *
 * <p>用于角色授权时的权限选择树：先按 permission_type 分组（菜单/按钮/接口/数据），
 * 组内按 parentCode 构建父子层级。
 */
@Data
public class OrganPermissionTreeVO {

    /** 节点编码（权限码或分组标识） */
    private String code;

    /** 节点名称 */
    private String name;

    /** 节点类型：分组节点为 group，权限节点为权限类型（1菜单 2按钮 3接口 4数据） */
    private Integer permissionType;

    /** 是否分组节点（true 表示按 permissionType 分组的虚拟根，无对应权限项） */
    private Boolean group;

    /** 权限编码（仅非分组节点有值，便于前端回显勾选） */
    private String permissionCode;

    /** 父权限编码 */
    private String parentCode;

    /** 图标 */
    private String icon;

    /** 排序号 */
    private Integer sortOrder;

    /** 子节点 */
    private List<OrganPermissionTreeVO> children = new ArrayList<>();
}
