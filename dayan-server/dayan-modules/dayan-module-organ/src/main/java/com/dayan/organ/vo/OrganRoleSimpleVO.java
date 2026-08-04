package com.dayan.organ.vo;

import lombok.Data;

/**
 * 角色精简 VO（列表项 / 下拉选择用，不含权限列表）。
 */
@Data
public class OrganRoleSimpleVO {

    /** 角色编码 */
    private String roleCode;

    /** 角色名称 */
    private String roleName;

    /** 所属组织编码 */
    private String organCode;

    /** 角色类型 */
    private Integer roleType;

    /** 状态 */
    private Integer status;

    /** 排序号 */
    private Integer sortOrder;
}
