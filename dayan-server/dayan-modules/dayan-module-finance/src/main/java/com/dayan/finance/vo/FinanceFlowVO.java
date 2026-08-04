package com.dayan.finance.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 财务流水（finance_flow）视图对象。
 *
 * <p>金额类原样返回 BigDecimal，无脱敏需求。
 */
@Data
public class FinanceFlowVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private String flowCode;
    /** 流水类型：1=收入/2=支出/3=退款/4=结算 */
    private Integer flowType;
    private String bizType;
    private String bizCode;
    private String accountType;
    private String accountCode;
    private BigDecimal flowAmount;
    private BigDecimal balanceBefore;
    private BigDecimal balanceAfter;
    private Integer payType;
    private String tradeNo;
    private String counterpartyType;
    private String counterpartyCode;
    private String counterpartyName;
    private String flowDescription;
    private LocalDateTime flowTime;
    /** 是否已结算：0=否/1=是 */
    private Integer isSettled;
    private String settleCode;
    /** 状态：0=已冲正/1=正常 */
    private Integer status;
    private String remark;
    private LocalDateTime createdAt;
}
