package com.dayan.finance.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 对账记录（finance_reconciliation）视图对象。
 */
@Data
public class FinanceReconciliationVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private String reconCode;
    /** 对账类型：1=渠道对账/2=供应商对账 */
    private Integer reconType;
    private String targetCode;
    private String targetName;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private Integer ourOrderCount;
    private BigDecimal ourTotalAmount;
    private Integer theirOrderCount;
    private BigDecimal theirTotalAmount;
    private Integer diffCount;
    private BigDecimal diffAmount;
    private String diffDetail;
    /** 对账结果：0=有差异/1=一致 */
    private Integer reconResult;
    private String handleResult;
    private LocalDateTime reconTime;
    private String operatorCode;
    private String operatorName;
    /** 状态：0=对账中/1=已完成/2=待确认/3=已确认 */
    private Integer status;
    private String remark;
    private LocalDateTime createdAt;
}
