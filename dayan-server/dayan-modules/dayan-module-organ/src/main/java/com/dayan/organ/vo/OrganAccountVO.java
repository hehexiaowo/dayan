package com.dayan.organ.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 账号 VO（列表 / 详情用）。
 *
 * <p>相比实体去除了 password / salt 等敏感字段，并附带机构名称与角色信息，
 * 供前端账号管理列表直接展示「机构」「角色」列，避免 N+1 查询。
 */
@Data
public class OrganAccountVO {

    private Long id;

    /** 所属组织编码 */
    private String organCode;

    /** 所属组织名称（organ_info.full_name 解析） */
    private String organName;

    /** 账号编码 */
    private String accountCode;

    /** 登录用户名 */
    private String username;

    /** 真实姓名 */
    private String realName;

    /** 头像URL */
    private String avatar;

    /** 性别 */
    private Integer gender;

    /** 手机号 */
    private String phone;

    /** 邮箱 */
    private String email;

    /** 最后登录时间 */
    private LocalDateTime lastLoginTime;

    /** 最后登录IP */
    private String lastLoginIp;

    /** 累计登录次数 */
    private Integer loginCount;

    /** 密码修改时间 */
    private LocalDateTime pwdUpdateTime;

    /** 账号状态：0锁定/1正常/2禁用 */
    private Integer accountStatus;

    /** 是否超级管理员：1是/0否 */
    private Integer isAdmin;

    /** 备注 */
    private String remark;

    /** 已分配角色编码列表（organ_account_role_rel 解析） */
    private List<String> roleCodes;

    /** 已分配角色名称列表（organ_role.role_name 解析） */
    private List<String> roleNames;
}
