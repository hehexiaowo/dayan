package com.dayan.finance.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 记录财务流水入参（finance_flow）。
 *
 * <p>流水编号、流水时间由服务端生成。balance_before/after 由服务端查询同账户最近一条 after 推导，
 * 若无历史则按 BigDecimal.ZERO 占位 + remark 说明（规格 §2.2）。
 */
@Data
public class RecordFlowDTO {

    /** 流水类型：1=收入/2=支出/3=退款/4=结算 */
    @NotNull(message = "流水类型不能为空")
    private Integer flowType;

    /** 业务类型字符串：equity_order/scene_order/course_order/travel_order/settlement */
    @NotBlank(message = "业务类型不能为空")
    private String bizType;

    /** 业务编码（可空） */
    private String bizCode;

    /** 账号类型：organ/channel/agent/client/supplier */
    @NotBlank(message = "账号类型不能为空")
    private String accountType;

    /** 账号编码 */
    @NotBlank(message = "账号编码不能为空")
    private String accountCode;

    /** 流水金额（正数） */
    @NotNull(message = "流水金额不能为空")
    private BigDecimal flowAmount;

    /** 支付方式（可空） */
    private Integer payType;

    /** 交易流水号（可空） */
    private String tradeNo;

    /** 对方类型 */
    private String counterpartyType;
    /** 对方编码 */
    private String counterpartyCode;
    /** 对方名称 */
    private String counterpartyName;

    /** 流水描述 */
    private String flowDescription;

    /** 备注 */
    private String remark;
}
