package com.dayan.supplier.dto;

import lombok.Data;

/**
 * 供应商账号查询入参。
 */
@Data
public class SupplierAccountQueryDTO {

    private Long current = 1L;
    private Long size = 20L;
    private String supplierCode;
    /** 用户名/手机号/邮箱模糊匹配 */
    private String username;
    private String realName;
    private Integer accountStatus;
    private Integer isAdmin;
}
