package com.dayan.supplier.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 供应商联系人视图对象。
 */
@Data
public class SupplierContactVO {

    private Long id;
    private String supplierCode;
    private String contactName;
    private Integer contactType;
    private String position;
    private String phone;
    private String email;
    private String wechat;
    private Integer isPrimary;
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
