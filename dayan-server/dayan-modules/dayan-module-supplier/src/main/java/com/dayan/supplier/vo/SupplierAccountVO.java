package com.dayan.supplier.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 供应商账号视图对象。
 */
@Data
public class SupplierAccountVO {

    private Long id;
    private String supplierCode;
    private String accountCode;
    private String username;
    private String realName;
    private String avatar;
    private String phone;
    private String openId;
    private String unionId;
    private String email;
    private String position;
    private LocalDateTime lastLoginTime;
    private String lastLoginIp;
    private Integer loginCount;
    private LocalDateTime pwdUpdateTime;
    private Integer accountStatus;
    private Integer isAdmin;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
