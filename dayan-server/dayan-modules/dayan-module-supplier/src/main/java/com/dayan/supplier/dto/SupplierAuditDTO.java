package com.dayan.supplier.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 供应商审核入参。
 *
 * <p>审核动作：待审核（status=1）→ 通过（status=2）/ 驳回（status=3）。
 */
@Data
public class SupplierAuditDTO {

    @NotBlank(message = "供应商编码不能为空")
    private String supplierCode;

    /** 审核状态：2=通过 / 3=驳回 */
    @NotNull(message = "审核状态不能为空")
    private Integer auditStatus;

    private String auditRemark;
}
