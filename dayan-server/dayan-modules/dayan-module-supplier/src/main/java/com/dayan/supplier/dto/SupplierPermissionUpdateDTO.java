package com.dayan.supplier.dto;

import lombok.Data;

/**
 * 供应商权限更新入参（{@code permissionCode} 不可改，由路径参数提供）。
 */
@Data
public class SupplierPermissionUpdateDTO {

    private String permissionName;
    private String parentCode;
    private Integer permissionType;
    private String path;
    private String method;
    private String icon;
    private Integer sortOrder;
    private Integer status;
    private String remark;
}
