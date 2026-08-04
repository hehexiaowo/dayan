package com.dayan.supplier.dto;

import lombok.Data;

import java.time.LocalDate;

/**
 * 供应商信息查询入参。
 */
@Data
public class SupplierInfoQueryDTO {

    private Long current = 1L;
    private Long size = 20L;
    private String supplierCode;
    private String fullName;
    private String shortName;
    private Integer supplierType;
    private String unifiedCreditCode;
    private Integer status;
    private Integer auditStatus;
}
