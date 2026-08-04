package com.dayan.supplier.dto;

import lombok.Data;

/**
 * 供应商开放平台配置查询入参。
 */
@Data
public class SupplierOpenPlatformQueryDTO {

    private Long current = 1L;
    private Long size = 20L;
    private String supplierCode;
    private String platformName;
    private Integer protocolType;
    private Integer authType;
    private Integer status;
}
