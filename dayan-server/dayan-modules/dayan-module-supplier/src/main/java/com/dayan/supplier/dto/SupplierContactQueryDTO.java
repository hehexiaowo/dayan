package com.dayan.supplier.dto;

import lombok.Data;

/**
 * 供应商联系人查询入参。
 */
@Data
public class SupplierContactQueryDTO {

    private Long current = 1L;
    private Long size = 20L;
    private String supplierCode;
    private String contactName;
    private Integer contactType;
    private Integer isPrimary;
}
