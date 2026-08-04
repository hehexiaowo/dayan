package com.dayan.supplier.service;

import com.dayan.supplier.dto.SupplierPermissionCreateDTO;
import com.dayan.supplier.dto.SupplierPermissionQueryDTO;
import com.dayan.supplier.dto.SupplierPermissionUpdateDTO;
import com.dayan.supplier.entity.SupplierPermission;
import com.dayan.common.core.resp.PageResult;

import java.util.List;

/**
 * 供应商权限服务（P3 仅 CRUD 框架）。
 *
 * <p>{@code permissionCode} 全局唯一，由前端按"模块:资源:动作"约定传入。
 */
public interface SupplierPermissionService {

    PageResult<SupplierPermission> page(SupplierPermissionQueryDTO query);

    SupplierPermission getDetail(String permissionCode);

    void create(SupplierPermissionCreateDTO dto);

    void update(String permissionCode, SupplierPermissionUpdateDTO dto);

    void delete(String permissionCode);

    /** 全量启用权限列表 */
    List<SupplierPermission> listAll();
}
