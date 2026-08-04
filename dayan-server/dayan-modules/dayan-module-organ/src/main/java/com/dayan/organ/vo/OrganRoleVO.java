package com.dayan.organ.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 角色详情 VO（含权限码列表）。
 */
@Data
public class OrganRoleVO {

    /** 主键 */
    private Long id;

    /** 所属组织编码 */
    private String organCode;

    /** 角色编码 */
    private String roleCode;

    /** 角色名称 */
    private String roleName;

    /** 角色类型 */
    private Integer roleType;

    /** 角色描述 */
    private String description;

    /** 数据范围 */
    private Integer dataScope;

    /** 状态（1启用 0停用） */
    private Integer status;

    /** 排序号 */
    private Integer sortOrder;

    /** 关联的权限码列表 */
    private List<String> permissionCodes;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}
