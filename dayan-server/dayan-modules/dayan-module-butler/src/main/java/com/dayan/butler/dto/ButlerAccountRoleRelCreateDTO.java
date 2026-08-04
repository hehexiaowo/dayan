package com.dayan.butler.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 管家账号-角色关联创建入参。
 */
@Data
public class ButlerAccountRoleRelCreateDTO {

    @NotBlank(message = "管家账号编码不能为空")
    private String accountCode;

    @NotBlank(message = "管家编码不能为空")
    private String butlerCode;

    private Integer roleType;
    private String description;
}
