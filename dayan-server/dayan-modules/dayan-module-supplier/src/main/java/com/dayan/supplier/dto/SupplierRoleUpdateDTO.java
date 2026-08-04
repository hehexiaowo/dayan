package com.dayan.supplier.dto;

import lombok.Data;

/**
 * 供应商角色更新入参（{@code roleCode} 不可改，由路径参数提供）。
 */
@Data
public class SupplierRoleUpdateDTO {

    private String roleName;
    private Integer roleType;
    private String description;
    private Integer status;
    private Integer sortOrder;
}
