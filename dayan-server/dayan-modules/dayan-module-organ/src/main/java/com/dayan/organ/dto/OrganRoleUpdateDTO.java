package com.dayan.organ.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 角色修改入参。
 *
 * <p>仅允许修改可变字段；roleCode/organCode 不可改。
 */
@Data
public class OrganRoleUpdateDTO {

    /** 角色名称 */
    @Size(max = 64, message = "角色名称长度不能超过 64")
    private String roleName;

    /** 角色类型 */
    private Integer roleType;

    /** 角色描述 */
    @Size(max = 255, message = "角色描述长度不能超过 255")
    private String description;

    /** 数据范围 */
    private Integer dataScope;

    /** 状态（1启用 0停用） */
    private Integer status;

    /** 排序号 */
    private Integer sortOrder;
}
