package com.dayan.organ.service;

import java.util.List;

/**
 * organ 域账号-角色关联服务。
 */
public interface OrganAccountRoleService {

    /**
     * 给账号分配角色（先删后增 account_role_rel，全量覆盖）。
     *
     * @param accountCode 账号编码
     * @param roleCodes   角色编码列表（全量覆盖）
     */
    void assignRoles(String accountCode, List<String> roleCodes);

    /**
     * 查询账号关联的角色编码列表。
     *
     * @param accountCode 账号编码
     * @return 角色编码列表
     */
    List<String> listRoles(String accountCode);
}
