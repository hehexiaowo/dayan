package com.dayan.finance.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 对账差异确认入参：2（待确认）→3（已确认）。
 */
@Data
public class ReconciliationConfirmDTO {

    @NotBlank(message = "对账编码不能为空")
    private String reconCode;

    /** 差异处理结果 */
    private String handleResult;

    /** 操作人编码 */
    private String operatorCode;
    /** 操作人姓名 */
    private String operatorName;

    /** 备注 */
    private String remark;
}
