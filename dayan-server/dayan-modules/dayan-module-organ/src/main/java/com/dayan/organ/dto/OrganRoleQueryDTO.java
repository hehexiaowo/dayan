package com.dayan.organ.dto;

import lombok.Data;

/**
 * 角色分页查询入参。
 *
 * <p>支持按 organCode/roleName/status 过滤，所有条件均为可选。
 */
@Data
public class OrganRoleQueryDTO {

    /** 组织编码（精确匹配，可选） */
    private String organCode;

    /** 角色名称（模糊匹配，可选） */
    private String roleName;

    /** 状态（可选） */
    private Integer status;

    /** 当前页码（从 1 开始，默认 1） */
    private Long current = 1L;

    /** 每页大小（默认 10） */
    private Long size = 10L;
}
