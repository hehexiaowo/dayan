package com.dayan.finance.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 结算单完成结算入参：2（结算中）→3（已结算）。
 */
@Data
public class BillFinishSettleDTO {

    @NotBlank(message = "结算单编号不能为空")
    private String billCode;

    /** 结算完成时间（为空时取当前时间） */
    private LocalDateTime settleTime;

    /** 备注 */
    private String remark;
}
