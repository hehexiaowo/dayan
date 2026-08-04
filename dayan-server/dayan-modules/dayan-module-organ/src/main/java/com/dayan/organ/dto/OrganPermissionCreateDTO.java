package com.dayan.organ.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 权限新增入参。
 *
 * <p>{@code permissionCode} 全局唯一，由前端按"模块:资源:动作"约定传入（如 {@code organ:role:list}）。
 */
@Data
public class OrganPermissionCreateDTO {

    /** 权限编码（全局唯一，如 organ:role:list） */
    @NotBlank(message = "权限编码不能为空")
    @Size(max = 128, message = "权限编码长度不能超过 128")
    private String permissionCode;

    /** 权限名称 */
    @NotBlank(message = "权限名称不能为空")
    @Size(max = 64, message = "权限名称长度不能超过 64")
    private String permissionName;

    /** 权限类型：1菜单 2按钮 3接口 4数据 */
    @NotNull(message = "权限类型不能为空")
    private Integer permissionType;

    /** 父权限编码（顶级为空字符串或 null） */
    @Size(max = 128, message = "父权限编码长度不能超过 128")
    private String parentCode;

    /** 路由/接口路径 */
    @Size(max = 255, message = "路径长度不能超过 255")
    private String path;

    /** 请求方法（GET/POST/PUT/DELETE，接口类型时填写） */
    @Size(max = 16, message = "请求方法长度不能超过 16")
    private String method;

    /** 图标（菜单类型时填写） */
    @Size(max = 64, message = "图标长度不能超过 64")
    private String icon;

    /** 排序号 */
    private Integer sortOrder;

    /** 状态（1启用 0停用，默认 1） */
    private Integer status;

    /** 备注 */
    @Size(max = 255, message = "备注长度不能超过 255")
    private String remark;
}
