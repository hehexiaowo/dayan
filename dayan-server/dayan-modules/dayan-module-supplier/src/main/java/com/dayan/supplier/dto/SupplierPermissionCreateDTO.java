package com.dayan.supplier.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 供应商权限创建入参。
 *
 * <p>{@code permissionCode} 全局唯一，由前端按"模块:资源:动作"约定传入。
 */
@Data
public class SupplierPermissionCreateDTO {

    @NotBlank(message = "权限编码不能为空")
    private String permissionCode;

    @NotBlank(message = "权限名称不能为空")
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
