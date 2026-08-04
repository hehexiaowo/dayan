package com.dayan.organ.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 权限修改入参。
 *
 * <p>{@code permissionCode} 不可改（全局唯一标识）。
 */
@Data
public class OrganPermissionUpdateDTO {

    /** 权限名称 */
    @Size(max = 64, message = "权限名称长度不能超过 64")
    private String permissionName;

    /** 权限类型：1菜单 2按钮 3接口 4数据 */
    private Integer permissionType;

    /** 父权限编码 */
    @Size(max = 128, message = "父权限编码长度不能超过 128")
    private String parentCode;

    /** 路由/接口路径 */
    @Size(max = 255, message = "路径长度不能超过 255")
    private String path;

    /** 请求方法 */
    @Size(max = 16, message = "请求方法长度不能超过 16")
    private String method;

    /** 图标 */
    @Size(max = 64, message = "图标长度不能超过 64")
    private String icon;

    /** 排序号 */
    private Integer sortOrder;

    /** 状态 */
    private Integer status;

    /** 备注 */
    @Size(max = 255, message = "备注长度不能超过 255")
    private String remark;
}
