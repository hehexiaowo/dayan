package com.dayan.common.core.statemachine;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 状态机转移规则（值对象）。
 *
 * <p>对应 system_state_machine 表的一行：domain + from_status + event → to_status。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StateRule {

    /** 状态机域标识（如 EQUITY_SM） */
    private String domain;
    /** 源状态 */
    private Integer fromStatus;
    /** 触发事件 */
    private String event;
    /** 目标状态 */
    private Integer toStatus;
    /** 备注 */
    private String remark;
}
