package com.dayan.finance.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 退款成功入参：2（退款中）→3（退款成功），写 refundTradeNo + refundTime。
 */
@Data
public class RefundMarkSuccessDTO {

    @NotBlank(message = "退款编码不能为空")
    private String refundCode;

    /** 退款交易号 */
    @NotBlank(message = "退款交易号不能为空")
    private String refundTradeNo;

    /** 退款完成时间（为空时取当前时间） */
    private java.time.LocalDateTime refundTime;

    /** 备注 */
    private String remark;
}
