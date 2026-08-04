package com.dayan.finance.dto;

import lombok.Data;

import java.time.LocalDate;

/**
 * 结算单（finance_bill）查询入参（分页 + 多条件）。
 */
@Data
public class FinanceBillQueryDTO {

    private Long current = 1L;
    private Long size = 20L;

    private String billCode;
    /** 结算类型：1=渠道结算/2=供应商结算 */
    private Integer billType;
    private String targetType;
    private String targetCode;
    /** 状态：0=待审核/1=审核通过/2=结算中/3=已结算/4=审核拒绝 */
    private Integer billStatus;
    /** 周期开始（大于等于） */
    private LocalDate periodStartFrom;
    /** 周期结束（小于等于） */
    private LocalDate periodEndTo;
}
