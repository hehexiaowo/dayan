package com.dayan.supplier.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 供应商角色创建入参。
 *
 * <p>{@code roleCode} 由系统生成（SR 前缀），同 supplierCode 内唯一。
 */
@Data
public class SupplierRoleCreateDTO {

    @NotBlank(message = "供应商编码不能为空")
    private String supplierCode;

    @NotBlank(message = "角色名称不能为空")
    private String roleName;

    private Integer roleType;
    private String description;
    private Integer status;
    private Integer sortOrder;
}
