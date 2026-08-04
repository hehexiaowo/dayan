package com.dayan.finance.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 退款审核入参：0（待审核）→1（通过）或 0→4（拒绝）。
 */
@Data
public class RefundAuditDTO {

    @NotBlank(message = "退款编码不能为空")
    private String refundCode;

    /** 是否通过：true=通过(→1)/false=拒绝(→4) */
    @NotNull(message = "审核结果不能为空")
    private Boolean pass;

    /** 审核人编码 */
    private String auditorCode;
    /** 审核人姓名 */
    private String auditorName;
    /** 审核备注 */
    private String auditRemark;
}
