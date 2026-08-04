package com.dayan.finance.dto;

import lombok.Data;

import java.time.LocalDate;

/**
 * 对账记录（finance_reconciliation）查询入参（分页 + 多条件）。
 */
@Data
public class FinanceReconciliationQueryDTO {

    private Long current = 1L;
    private Long size = 20L;

    private String reconCode;
    /** 对账类型：1=渠道对账/2=供应商对账 */
    private Integer reconType;
    private String targetCode;
    /** 对账结果：0=有差异/1=一致 */
    private Integer reconResult;
    /** 状态：0=对账中/1=已完成/2=待确认/3=已确认 */
    private Integer status;
    /** 周期开始（大于等于） */
    private LocalDate periodStartFrom;
    /** 周期结束（小于等于） */
    private LocalDate periodEndTo;
}
