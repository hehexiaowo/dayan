package com.dayan.finance.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 生成结算单（finance_bill）入参。
 *
 * <p>由调用方传入周期内汇总信息（订单数/总额/手续费/退款额/调整额），
 * 服务端按 final = total - commission - refund + adjust 计算最终金额，并置 bill_status=0。
 */
@Data
public class GenerateBillDTO {

    /** 结算类型：1=渠道结算/2=供应商结算 */
    @NotNull(message = "结算类型不能为空")
    private Integer billType;

    /** 结算对象类型：channel/supplier/distributor */
    @NotBlank(message = "结算对象类型不能为空")
    private String targetType;

    /** 结算对象编码 */
    @NotBlank(message = "结算对象编码不能为空")
    private String targetCode;

    /** 结算对象名称 */
    @NotBlank(message = "结算对象名称不能为空")
    private String targetName;

    /** 结算周期开始 */
    @NotNull(message = "结算周期开始不能为空")
    private LocalDate periodStart;

    /** 结算周期结束 */
    @NotNull(message = "结算周期结束不能为空")
    private LocalDate periodEnd;

    /** 订单数量 */
    @NotNull(message = "订单数量不能为空")
    private Integer orderCount;

    /** 结算总额 */
    @NotNull(message = "结算总额不能为空")
    private BigDecimal totalAmount;

    /** 分销手续费金额（默认 0） */
    private BigDecimal commissionAmount;
    /** 退款金额（默认 0） */
    private BigDecimal refundAmount;
    /** 调整金额（默认 0） */
    private BigDecimal adjustAmount;

    /** 关联流水 ID 列表（JSON 数组字符串，可空） */
    private List<Long> flowIds;

    /** 结算方式：1=银行转账/2=线上转账（默认 1） */
    private Integer settlementMethod;
    /** 收款银行信息 */
    private String bankInfo;

    /** 备注 */
    private String remark;
}
