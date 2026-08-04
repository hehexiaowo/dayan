package com.dayan.common.core.statemachine;

import java.util.List;

/**
 * 状态机规则数据源接口。
 *
 * <p>由 system 业务模块实现（读取 system_state_machine 表），提供给
 * {@link StateMachineEngine} 加载规则。common 层不直接依赖业务表。
 */
public interface StateRuleLoader {

    /**
     * 查询指定域的全部状态转移规则。
     *
     * @param domain 状态机域标识
     * @return 规则列表
     */
    List<StateRule> loadByDomain(String domain);

    /**
     * 查询全部域的全部规则（启动预热用）。
     */
    List<StateRule> loadAll();
}
