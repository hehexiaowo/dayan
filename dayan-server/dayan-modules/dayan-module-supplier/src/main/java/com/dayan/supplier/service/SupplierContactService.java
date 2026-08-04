package com.dayan.supplier.service;

import com.dayan.supplier.dto.SupplierContactCreateDTO;
import com.dayan.supplier.dto.SupplierContactQueryDTO;
import com.dayan.supplier.dto.SupplierContactUpdateDTO;
import com.dayan.supplier.vo.SupplierContactVO;
import com.dayan.common.core.resp.PageResult;

/**
 * 供应商联系人服务。
 *
 * <p>主联系人唯一：同 supplierCode 下 {@code isPrimary=1} 仅 1 个。
 */
public interface SupplierContactService {

    PageResult<SupplierContactVO> page(SupplierContactQueryDTO query);

    SupplierContactVO getDetail(Long id);

    Long create(SupplierContactCreateDTO dto);

    void update(Long id, SupplierContactUpdateDTO dto);

    void delete(Long id);
}
