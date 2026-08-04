package com.dayan.supplier.dto;

import lombok.Data;

/**
 * 供应商权限查询入参。
 */
@Data
public class SupplierPermissionQueryDTO {

    private Long current = 1L;
    private Long size = 20L;
    private String permissionName;
    private Integer permissionType;
    private Integer status;
}
