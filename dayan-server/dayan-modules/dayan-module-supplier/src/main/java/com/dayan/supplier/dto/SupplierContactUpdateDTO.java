package com.dayan.supplier.dto;

import lombok.Data;

/**
 * 供应商联系人更新入参（{@code id} 由路径参数提供）。
 */
@Data
public class SupplierContactUpdateDTO {

    private String contactName;
    private Integer contactType;
    private String position;
    private String phone;
    private String email;
    private String wechat;
    private Integer isPrimary;
    private String remark;
}
