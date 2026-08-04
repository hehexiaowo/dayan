package com.dayan.finance.vo;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 结算单（finance_bill）视图对象。
 */
@Data
public class FinanceBillVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private String billCode;
    /** 结算类型：1=渠道结算/2=供应商结算 */
    private Integer billType;
    private String targetType;
    private String targetCode;
    private String targetName;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private Integer orderCount;
    private BigDecimal totalAmount;
    private BigDecimal commissionAmount;
    private BigDecimal refundAmount;
    private BigDecimal adjustAmount;
    private BigDecimal finalAmount;
    /** 关联流水ID列表（JSON 数组字符串） */
    private String flowIds;
    /** 结算方式：1=银行转账/2=线上转账 */
    private Integer settlementMethod;
    private String bankInfo;
    private LocalDateTime applyTime;
    private LocalDateTime auditTime;
    private LocalDateTime settleTime;
    private String auditorCode;
    private String auditorName;
    private String auditRemark;
    /** 状态：0=待审核/1=审核通过/2=结算中/3=已结算/4=审核拒绝 */
    private Integer billStatus;
    private String remark;
    private LocalDateTime createdAt;
}
