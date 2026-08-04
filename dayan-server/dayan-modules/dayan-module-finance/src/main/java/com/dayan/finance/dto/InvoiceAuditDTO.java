package com.dayan.finance.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 发票审核入参：0→1（已审核）。
 */
@Data
public class InvoiceAuditDTO {

    @NotBlank(message = "发票编码不能为空")
    private String invoiceCode;

    /** 备注 */
    private String remark;
}
