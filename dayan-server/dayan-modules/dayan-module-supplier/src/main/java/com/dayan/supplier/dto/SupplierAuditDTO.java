package com.dayan.supplier.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 供应商审核入参。
 *
 * <p>审核动作：对 status=0（待审核）的供应商进行审核。
 * 审核通过则 audit_status=1（status 升级为 1=已合作）；
 * 审核驳回则 audit_status=2（status 维持 0=待审核）。
 * 入参 {@link #auditStatus} 仅支持 1=审核通过 / 2=审核驳回。
 */
@Data
public class SupplierAuditDTO {

    @NotBlank(message = "供应商编码不能为空")
    private String supplierCode;

    /** 审核状态：1=审核通过 / 2=审核驳回 */
    @NotNull(message = "审核状态不能为空")
    private Integer auditStatus;

    private String auditRemark;
}
