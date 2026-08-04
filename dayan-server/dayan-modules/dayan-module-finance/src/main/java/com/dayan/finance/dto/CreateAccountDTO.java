package com.dayan.finance.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 创建应收应付账目（finance_account）入参。
 *
 * <p>账目编号由服务端生成，received_amount=0、remain_amount=total_amount、account_status=0（待收/付）。
 */
@Data
public class CreateAccountDTO {

    /** 账目方向：1=应收/2=应付 */
    @NotNull(message = "账目方向不能为空")
    private Integer direction;

    /** 对象类型：channel/supplier/agent */
    @NotBlank(message = "对象类型不能为空")
    private String accountType;

    /** 对象编码 */
    @NotBlank(message = "对象编码不能为空")
    private String targetCode;

    /** 对象名称 */
    @NotBlank(message = "对象名称不能为空")
    private String targetName;

    /** 业务类型：equity_purchase/scene_fee/service_fee */
    @NotBlank(message = "业务类型不能为空")
    private String bizType;

    /** 业务编码（可空） */
    private String bizCode;

    /** 应收/应付总额 */
    @NotNull(message = "总额不能为空")
    private BigDecimal totalAmount;

    /** 到期日期 */
    private LocalDate dueDate;

    /** 备注 */
    private String remark;
}
