package com.dayan.finance.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 应收应付账目（finance_account）视图对象。
 */
@Data
public class FinanceAccountVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private String accountCode;
    /** 账目方向：1=应收/2=应付 */
    private Integer direction;
    private String accountType;
    private String targetCode;
    private String targetName;
    private String bizType;
    private String bizCode;
    private BigDecimal totalAmount;
    private BigDecimal receivedAmount;
    private BigDecimal remainAmount;
    private LocalDate dueDate;
    private LocalDateTime lastReceiveTime;
    /** 状态：0=待收付/1=部分收付/2=已结清/3=已逾期/4=已坏账 */
    private Integer accountStatus;
    private String remark;
    private LocalDateTime createdAt;
}
