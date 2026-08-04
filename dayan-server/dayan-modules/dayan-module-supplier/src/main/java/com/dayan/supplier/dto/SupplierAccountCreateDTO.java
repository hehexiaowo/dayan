package com.dayan.supplier.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 供应商账号创建入参。
 *
 * <p>{@code accountCode} 由系统生成（SA 前缀），密码经 BCrypt 哈希存储。
 * {@code isAdmin=1} 主账号：同 supplierCode 下仅允许 1 个主账号（创建时若标记为主账号，
 * 先把同 supplierCode 下其他账号 isAdmin 置 0）。
 */
@Data
public class SupplierAccountCreateDTO {

    @NotBlank(message = "供应商编码不能为空")
    private String supplierCode;

    @NotBlank(message = "用户名不能为空")
    @Size(max = 64)
    private String username;

    /** 明文密码，为空则使用默认值 */
    private String password;

    private String realName;
    private String avatar;
    private String phone;
    private String openId;
    private String unionId;
    private String email;
    private String position;
    private Integer accountStatus;
    private Integer isAdmin;
}
