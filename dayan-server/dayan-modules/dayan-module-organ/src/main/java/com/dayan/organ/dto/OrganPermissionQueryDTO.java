package com.dayan.organ.dto;

import lombok.Data;

/**
 * 权限分页查询入参。
 *
 * <p>支持按 permissionType / permissionName / status 过滤，所有条件均为可选。
 */
@Data
public class OrganPermissionQueryDTO {

    /** 权限类型（1菜单 2按钮 3接口 4数据，可选） */
    private Integer permissionType;

    /** 权限名称（模糊匹配，可选） */
    private String permissionName;

    /** 状态（可选） */
    private Integer status;

    /** 当前页码（从 1 开始，默认 1） */
    private Long current = 1L;

    /** 每页大小（默认 10） */
    private Long size = 10L;
}
