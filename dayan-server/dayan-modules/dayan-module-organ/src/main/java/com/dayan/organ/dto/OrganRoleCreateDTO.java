package com.dayan.organ.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 角色新增入参。
 *
 * <p>{@code roleCode} 由后端通过 CodeGenerator 生成（OR 前缀），前端无需传入。
 */
@Data
public class OrganRoleCreateDTO {

    /** 所属组织编码（平台超管可传平台编码） */
    @NotBlank(message = "组织编码不能为空")
    @Size(max = 64, message = "组织编码长度不能超过 64")
    private String organCode;

    /** 角色名称 */
    @NotBlank(message = "角色名称不能为空")
    @Size(max = 64, message = "角色名称长度不能超过 64")
    private String roleName;

    /** 角色类型 */
    private Integer roleType;

    /** 角色描述 */
    @Size(max = 255, message = "角色描述长度不能超过 255")
    private String description;

    /** 数据范围 */
    private Integer dataScope;

    /** 状态（1启用 0停用，默认 1） */
    private Integer status;

    /** 排序号 */
    private Integer sortOrder;

    /** 创建时一并授权的权限码列表（可选） */
    private List<String> permissionCodes;
}
