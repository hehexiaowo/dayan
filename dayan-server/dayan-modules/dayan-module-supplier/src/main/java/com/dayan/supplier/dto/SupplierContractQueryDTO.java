package com.dayan.supplier.dto;

import lombok.Data;

/**
 * 供应商合同查询入参。
 */
@Data
public class SupplierContractQueryDTO {

    private Long current = 1L;
    private Long size = 20L;
    private String contractCode;
    private String contractName;
    private String supplierCode;
    private String organCode;
    private Integer contractType;
    private Integer settlementCycle;
    private Integer status;
    /** 是否查询某合同的全部续约链（按 parentContractCode 回溯/下钻） */
    private String parentContractCode;
}
