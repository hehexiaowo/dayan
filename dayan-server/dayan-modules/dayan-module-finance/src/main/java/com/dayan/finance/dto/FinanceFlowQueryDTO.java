package com.dayan.finance.dto;

import lombok.Data;

/**
 * 财务流水（finance_flow）查询入参（分页 + 多条件）。
 *
 * <p>仿照 {@code EquityDepotQueryDTO} 结构：自含 current/size。
 */
@Data
public class FinanceFlowQueryDTO {

    private Long current = 1L;
    private Long size = 20L;

    private String flowCode;
    /** 流水类型 */
    private Integer flowType;
    private String bizType;
    private String bizCode;
    private String accountType;
    private String accountCode;
    /** 状态：0=已冲正/1=正常 */
    private Integer status;
    /** 是否已结算：0=否/1=是 */
    private Integer isSettled;
    private String settleCode;
}
