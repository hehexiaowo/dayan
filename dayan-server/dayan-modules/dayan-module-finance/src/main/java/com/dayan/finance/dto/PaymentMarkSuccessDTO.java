package com.dayan.finance.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 支付成功入参：0（待支付）→1（支付成功），写 tradeNo + payTime + notifyTime。
 */
@Data
public class PaymentMarkSuccessDTO {

    @NotBlank(message = "支付流水号不能为空")
    private String paymentCode;

    /** 第三方交易号 */
    @NotBlank(message = "第三方交易号不能为空")
    private String tradeNo;

    /** 支付时间（为空时取当前时间） */
    private LocalDateTime payTime;

    /** 回调通知时间（为空时取当前时间） */
    private LocalDateTime notifyTime;

    /** 付款方账号 */
    private String payerAccount;
    /** 收款方账号 */
    private String payeeAccount;

    /** 备注 */
    private String payDescription;
}
