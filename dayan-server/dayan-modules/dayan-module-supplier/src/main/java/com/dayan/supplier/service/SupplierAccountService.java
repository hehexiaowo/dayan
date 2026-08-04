package com.dayan.supplier.service;

import com.dayan.supplier.dto.SupplierAccountCreateDTO;
import com.dayan.supplier.dto.SupplierAccountQueryDTO;
import com.dayan.supplier.dto.SupplierAccountUpdateDTO;
import com.dayan.supplier.vo.SupplierAccountVO;
import com.dayan.common.core.resp.PageResult;

/**
 * 供应商账号服务。
 *
 * <p>密码使用 BCrypt（复用 {@code common-security.PasswordService}）。
 * 主账号（{@code isAdmin=1}）同 supplierCode 下仅 1 个。
 */
public interface SupplierAccountService {

    PageResult<SupplierAccountVO> page(SupplierAccountQueryDTO query);

    SupplierAccountVO getDetail(String accountCode);

    String create(SupplierAccountCreateDTO dto);

    void update(String accountCode, SupplierAccountUpdateDTO dto);

    void resetPassword(String accountCode);

    void delete(String accountCode);
}
