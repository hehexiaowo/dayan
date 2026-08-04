package com.dayan.finance.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 退款失败入参：2（退款中）→5（退款失败）。
 */
@Data
public class RefundMarkFailedDTO {

    @NotBlank(message = "退款编码不能为空")
    private String refundCode;

    /** 备注（失败原因） */
    private String remark;
}
