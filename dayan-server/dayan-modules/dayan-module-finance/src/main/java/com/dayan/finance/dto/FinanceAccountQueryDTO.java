package com.dayan.finance.dto;

import lombok.Data;

import java.time.LocalDate;

/**
 * 应收应付账目（finance_account）查询入参（分页 + 多条件）。
 */
@Data
public class FinanceAccountQueryDTO {

    private Long current = 1L;
    private Long size = 20L;

    private String accountCode;
    /** 账目方向：1=应收/2=应付 */
    private Integer direction;
    private String accountType;
    private String targetCode;
    private String bizType;
    private String bizCode;
    /** 状态：0=待收付/1=部分收付/2=已结清/3=已逾期/4=已坏账 */
    private Integer accountStatus;
    /** 到期日期（小于等于） */
    private LocalDate dueDateTo;
}
