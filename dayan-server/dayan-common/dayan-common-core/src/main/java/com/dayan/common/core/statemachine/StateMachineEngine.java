package com.dayan.common.core.statemachine;

/**
 * 状态机引擎接口。
 *
 * <p>各业务域（权益/订单/服务会话/机构）的状态流转统一通过本引擎校验，
 * 禁止应用层直接 UPDATE status 字段，强制走状态机保证流转合法性。
 *
 * <p>规则存储于 {@code system_state_machine} 表（domain/from_status/event/to_status），
 * 启动时加载到缓存，DB 变更后调用 {@link #refreshRules} 刷新。
 */
public interface StateMachineEngine {

    /**
     * 校验状态转移合法性。非法则抛 BusinessException。
     *
     * @param domain 状态机域标识，如 {@code "EQUITY_SM"}
     * @param from   当前状态
     * @param event  触发事件
     * @return 目标状态 to
     */
    int checkTransition(String domain, int from, String event);

    /**
     * 执行状态转移：校验合法后返回 to。不修改 DB，由调用方落库。
     *
     * @param domain 状态机域标识
     * @param from   当前状态
     * @param event  触发事件
     * @return 目标状态 to
     */
    int transition(String domain, int from, String event);

    /**
     * 加载某域全部规则到缓存（应用启动时调用）。
     */
    void loadRules(String domain);

    /**
     * 刷新某域规则缓存（DB 变更后调用）。
     */
    void refreshRules(String domain);
}
