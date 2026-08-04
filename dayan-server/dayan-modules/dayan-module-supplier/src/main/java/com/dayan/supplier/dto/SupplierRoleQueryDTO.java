package com.dayan.supplier.dto;

import lombok.Data;

/**
 * 供应商角色查询入参。
 */
@Data
public class SupplierRoleQueryDTO {

    private Long current = 1L;
    private Long size = 20L;
    private String supplierCode;
    private String roleName;
    private Integer roleType;
    private Integer status;
}
