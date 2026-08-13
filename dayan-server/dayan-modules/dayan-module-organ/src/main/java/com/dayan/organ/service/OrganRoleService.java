package com.dayan.organ.service;

import com.dayan.common.core.resp.PageResult;
import com.dayan.organ.dto.OrganRoleCreateDTO;
import com.dayan.organ.dto.OrganRoleGrantsDTO;
import com.dayan.organ.dto.OrganRoleQueryDTO;
import com.dayan.organ.dto.OrganRoleUpdateDTO;
import com.dayan.organ.vo.OrganRoleSimpleVO;
import com.dayan.organ.vo.OrganRoleVO;

import java.util.List;

/**
 * organ 域角色管理服务。
 */
public interface OrganRoleService {

    /**
     * 分页查询角色。
     *
     * @param query 过滤条件（organCode/roleName/status）
     * @return 角色精简列表分页
     */
    PageResult<OrganRoleSimpleVO> page(OrganRoleQueryDTO query);

    /**
     * 查询角色详情（含权限码列表）。
     *
     * @param roleCode 角色编码
     * @return 角色详情
     */
    OrganRoleVO getDetail(String roleCode);

    /**
     * 新增角色。
     *
     * <p>roleCode 由 CodeGenerator 生成（OR 前缀），校验 organCode+roleCode 唯一。
     * 若 dto.permissionCodes 非空，则一并完成授权。
     *
     * @param dto 角色新增入参
     * @return 生成的角色编码
     */
    String create(OrganRoleCreateDTO dto);

    /**
     * 修改角色。
     *
     * @param roleCode 角色编码
     * @param dto      修改入参
     */
    void update(String roleCode, OrganRoleUpdateDTO dto);

    /**
     * 逻辑删除角色。
     *
     * <p>校验是否有账号关联，有则拒绝删除。
     *
     * @param roleCode 角色编码
     */
    void delete(String roleCode);

    /**
     * 给角色授权（菜单+权限，全量覆盖，单事务先删后插）。
     */
    void assignGrants(String roleCode, OrganRoleGrantsDTO grants);

    /**
     * 查询角色的权限码列表。
     *
     * @param roleCode 角色编码
     * @return 权限码列表
     */
    List<String> listPermissions(String roleCode);

    /**
     * 查询角色授权（菜单码+权限码），供分配权限弹窗回显。
     */
    OrganRoleGrantsDTO listGrants(String roleCode);
}
