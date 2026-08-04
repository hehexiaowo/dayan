package com.dayan.supplier.service;

import com.dayan.supplier.dto.SupplierRoleCreateDTO;
import com.dayan.supplier.dto.SupplierRoleQueryDTO;
import com.dayan.supplier.dto.SupplierRoleUpdateDTO;
import com.dayan.supplier.entity.SupplierRole;
import com.dayan.common.core.resp.PageResult;

import java.util.List;

/**
 * 供应商角色服务（P3 仅 CRUD 框架 + 权限授权）。
 *
 * <p>角色编码 SR + 5 位序列（如 SR00001），全局唯一。授权采用"先删后增"全量覆盖语义。
 * RBAC 鉴权回调（SupplierStpInterface）后置实现。
 */
public interface SupplierRoleService {

    PageResult<SupplierRole> page(SupplierRoleQueryDTO query);

    SupplierRole getDetail(String roleCode);

    String create(SupplierRoleCreateDTO dto);

    void update(String roleCode, SupplierRoleUpdateDTO dto);

    void delete(String roleCode);

    /** 给角色授权（全量覆盖） */
    void assignPermissions(String roleCode, List<String> permissionCodes);

    /** 查询角色权限码列表 */
    List<String> listPermissions(String roleCode);
}
