package com.dayan.finance.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 支付失败入参：0（待支付）→2（支付失败）。
 */
@Data
public class PaymentMarkFailedDTO {

    @NotBlank(message = "支付流水号不能为空")
    private String paymentCode;

    /** 支付说明（失败原因） */
    private String payDescription;

    /** 回调通知时间 */
    private java.time.LocalDateTime notifyTime;
}
