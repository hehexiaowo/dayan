package com.dayan.supplier.service;

import com.dayan.supplier.dto.SupplierContractCreateDTO;
import com.dayan.supplier.dto.SupplierContractQueryDTO;
import com.dayan.supplier.dto.SupplierContractUpdateDTO;
import com.dayan.supplier.vo.SupplierContractVO;
import com.dayan.common.core.resp.PageResult;

/**
 * 供应商合同服务。
 *
 * <p>合同编码 HT+5 位（{@code codeGenerator.generate("HT")}），全局唯一。
 * 续约链：{@code parentContractCode} 指向原合同，原合同 {@code renewCount} +1。
 * 日期校验：{@code effectiveDate < expireDate}。
 */
public interface SupplierContractService {

    PageResult<SupplierContractVO> page(SupplierContractQueryDTO query);

    SupplierContractVO getDetail(String contractCode);

    String create(SupplierContractCreateDTO dto);

    void update(String contractCode, SupplierContractUpdateDTO dto);

    void delete(String contractCode);
}
