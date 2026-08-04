package com.dayan.organ.service;

import com.dayan.common.core.resp.PageResult;
import com.dayan.organ.dto.OrganPermissionCreateDTO;
import com.dayan.organ.dto.OrganPermissionQueryDTO;
import com.dayan.organ.dto.OrganPermissionUpdateDTO;
import com.dayan.organ.vo.OrganPermissionTreeVO;
import com.dayan.organ.vo.OrganPermissionVO;

import java.util.List;

/**
 * organ 域权限项管理服务。
 */
public interface OrganPermissionService {

    /**
     * 分页查询权限项。
     *
     * @param query 过滤条件（permissionType/permissionName/status）
     * @return 权限项分页
     */
    PageResult<OrganPermissionVO> page(OrganPermissionQueryDTO query);

    /**
     * 新增权限项（permissionCode 全局唯一校验）。
     *
     * @param dto 权限新增入参
     */
    void create(OrganPermissionCreateDTO dto);

    /**
     * 修改权限项（permissionCode 不可改）。
     *
     * @param permissionCode 权限编码
     * @param dto            修改入参
     */
    void update(String permissionCode, OrganPermissionUpdateDTO dto);

    /**
     * 逻辑删除权限项。
     *
     * @param permissionCode 权限编码
     */
    void delete(String permissionCode);

    /**
     * 查全部启用权限项（供角色授权选择）。
     *
     * @return 权限项列表
     */
    List<OrganPermissionVO> listAll();

    /**
     * 按权限类型分组返回权限树（供角色授权勾选）。
     *
     * @return 权限树（根为按类型分组的虚拟节点）
     */
    List<OrganPermissionTreeVO> tree();
}
