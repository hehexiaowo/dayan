package com.dayan.supplier.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 供应商联系人创建入参。
 *
 * <p>{@code isPrimary=1} 主联系人：同 supplierCode 下仅 1 个（创建/更新时若设为主联系人，
 * 先把同 supplierCode 下其他联系人 isPrimary 置 0）。
 */
@Data
public class SupplierContactCreateDTO {

    @NotBlank(message = "供应商编码不能为空")
    private String supplierCode;

    @NotBlank(message = "联系人姓名不能为空")
    private String contactName;

    /** 联系人类型：1=商务 / 2=财务 / 3=运营 / 4=其他 */
    private Integer contactType;

    private String position;
    private String phone;
    private String email;
    private String wechat;
    private Integer isPrimary;
    private String remark;
}
