package com.dayan.finance.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 结算单审核入参：0→1（通过）或 0→4（拒绝）。
 */
@Data
public class BillAuditDTO {

    @NotBlank(message = "结算单编号不能为空")
    private String billCode;

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
