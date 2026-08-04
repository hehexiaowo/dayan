package com.dayan.finance.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 对账提交差异入参：0（对账中）→2（待确认）。
 */
@Data
public class ReconciliationSubmitDiffDTO {

    @NotBlank(message = "对账编码不能为空")
    private String reconCode;

    /** 差异明细（JSON 字符串） */
    private String diffDetail;
    /** 差异处理结果 */
    private String handleResult;

    /** 操作人编码 */
    private String operatorCode;
    /** 操作人姓名 */
    private String operatorName;

    /** 备注 */
    private String remark;
}
