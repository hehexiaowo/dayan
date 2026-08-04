package com.dayan.finance.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 创建对账记录（finance_reconciliation）入参。
 *
 * <p>对账编号、对账时间由服务端生成，status=0（对账中）。
 */
@Data
public class CreateReconciliationDTO {

    /** 对账类型：1=渠道对账/2=供应商对账 */
    @NotNull(message = "对账类型不能为空")
    private Integer reconType;

    /** 对账对象编码 */
    @NotBlank(message = "对账对象编码不能为空")
    private String targetCode;

    /** 对账对象名称 */
    @NotBlank(message = "对账对象名称不能为空")
    private String targetName;

    /** 对账周期开始 */
    @NotNull(message = "对账周期开始不能为空")
    private LocalDate periodStart;

    /** 对账周期结束 */
    @NotNull(message = "对账周期结束不能为空")
    private LocalDate periodEnd;

    /** 我方订单数 */
    @NotNull(message = "我方订单数不能为空")
    private Integer ourOrderCount;

    /** 我方总金额 */
    @NotNull(message = "我方总金额不能为空")
    private BigDecimal ourTotalAmount;

    /** 对方订单数（可空） */
    private Integer theirOrderCount;
    /** 对方总金额（可空） */
    private BigDecimal theirTotalAmount;

    /** 差异订单数（默认 0） */
    private Integer diffCount;
    /** 差异金额（默认 0） */
    private BigDecimal diffAmount;
    /** 差异明细（JSON 字符串） */
    private String diffDetail;

    /** 对账结果：0=有差异/1=一致（默认 0） */
    private Integer reconResult;

    /** 操作人编码 */
    @NotBlank(message = "操作人编码不能为空")
    private String operatorCode;
    /** 操作人姓名 */
    private String operatorName;

    /** 对账时间（为空时取当前时间） */
    private LocalDateTime reconTime;

    /** 备注 */
    private String remark;
}
