package com.dayan.finance.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 发票开具入参：1（已审核）→2（已开票），写 invoiceNo + issueTime + invoiceUrl。
 */
@Data
public class InvoiceIssueDTO {

    @NotBlank(message = "发票编码不能为空")
    private String invoiceCode;

    /** 发票号码（税务号码） */
    @NotBlank(message = "发票号码不能为空")
    private String invoiceNo;

    /** 发票文件 URL */
    private String invoiceUrl;

    /** 备注 */
    private String remark;
}
