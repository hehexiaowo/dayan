package com.dayan.finance.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 发票寄出入参：2（已开票）→3（已寄出），写 sendTime。
 */
@Data
public class InvoiceSendDTO {

    @NotBlank(message = "发票编码不能为空")
    private String invoiceCode;

    /** 备注 */
    private String remark;
}
