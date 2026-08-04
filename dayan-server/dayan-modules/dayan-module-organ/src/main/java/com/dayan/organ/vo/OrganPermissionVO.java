package com.dayan.organ.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 权限项 VO。
 */
@Data
public class OrganPermissionVO {

    /** 主键 */
    private Long id;

    /** 权限编码（全局唯一） */
    private String permissionCode;

    /** 权限名称 */
    private String permissionName;

    /** 父权限编码 */
    private String parentCode;

    /** 权限类型：1菜单 2按钮 3接口 4数据 */
    private Integer permissionType;

    /** 路由/接口路径 */
    private String path;

    /** 请求方法 */
    private String method;

    /** 图标 */
    private String icon;

    /** 排序号 */
    private Integer sortOrder;

    /** 状态（1启用 0停用） */
    private Integer status;

    /** 备注 */
    private String remark;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}
