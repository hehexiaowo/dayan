package com.dayan.organ.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 角色授权收发结构（菜单可见性 + 接口权限）。
 *
 * <p>menuCodes 写 organ_role_menu_rel（控制 /menus/mine 侧边栏与动态路由）；
 * permissionCodes 写 organ_role_permission_ship（控制 @SaCheckPermission 接口放行）。
 */
@Data
public class OrganRoleGrantsDTO {

    /** 菜单编码集合（system_menu.menu_code，可含目录） */
    private List<String> menuCodes = new ArrayList<>();

    /** 权限编码集合（organ_permission.permission_code） */
    private List<String> permissionCodes = new ArrayList<>();
}
