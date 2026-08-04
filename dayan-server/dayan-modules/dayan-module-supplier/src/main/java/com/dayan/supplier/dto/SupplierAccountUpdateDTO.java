package com.dayan.supplier.dto;

import lombok.Data;

/**
 * 供应商账号更新入参（{@code accountCode} 不可改，由路径参数提供）。
 */
@Data
public class SupplierAccountUpdateDTO {

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
