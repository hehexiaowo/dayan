package com.dayan.finance.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 发票作废 / 红冲入参。
 *
 * <p>作废（void）：→5；红冲（redFlush）：→6。服务端按调用方法区分目标状态。
 */
@Data
public class InvoiceOperateDTO {

    @NotBlank(message = "发票编码不能为空")
    private String invoiceCode;

    /** 备注 */
    private String remark;
}
