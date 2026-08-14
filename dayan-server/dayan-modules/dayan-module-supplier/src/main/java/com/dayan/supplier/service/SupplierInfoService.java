package com.dayan.supplier.service;

import com.dayan.supplier.dto.SupplierAuditDTO;
import com.dayan.supplier.dto.SupplierInfoCreateDTO;
import com.dayan.supplier.dto.SupplierInfoQueryDTO;
import com.dayan.supplier.dto.SupplierInfoUpdateDTO;
import com.dayan.supplier.vo.SupplierInfoVO;
import com.dayan.common.core.resp.PageResult;

import java.util.List;

/**
 * 供应商信息服务。
 *
 * <p>{@code supplier_info} 为平台共享表（DayanTenantHandler 忽略），不参与渠道字段隔离。
 * 审核流：status 1=待审核 / 2=已通过 / 3=已驳回。
 */
public interface SupplierInfoService {

    /**
     * 分页查询。
     */
    PageResult<SupplierInfoVO> page(SupplierInfoQueryDTO query);

    /**
     * 全量列表（不分页，下拉/关联选择用）。
     */
    List<SupplierInfoVO> list(SupplierInfoQueryDTO query);

    /**
     * 详情。
     */
    SupplierInfoVO getDetail(String supplierCode);

    /**
     * 新增供应商，返回生成的 supplierCode。
     */
    String create(SupplierInfoCreateDTO dto);

    /**
     * 修改供应商。
     */
    void update(String supplierCode, SupplierInfoUpdateDTO dto);

    /**
     * 删除供应商。
     */
    void delete(String supplierCode);

    /**
     * 审核供应商（待审核 → 通过/驳回）。
     */
    void audit(SupplierAuditDTO dto);
}
