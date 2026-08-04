package com.dayan.common.security;

/**
 * 账号类型（四端 + 预留两端）。
 *
 * <p>对应 Sa-Token 的多账号体系，每端独立 Token 命名空间，互不串扰。
 */
public enum AccountType {

    /** Admin 运营端（organ_account） */
    ADMIN("admin", "Admin-Token"),
    /** Channel 渠道端（channel_account） */
    CHANNEL("channel", "Channel-Token"),
    /** Agent 代理人端（agent_account） */
    AGENT("agent", "Agent-Token"),
    /** Client 客户端（client_account） */
    CLIENT("client", "Client-Token"),
    /** Supplier 供应商端（supplier_account，预留） */
    SUPPLIER("supplier", "Supplier-Token"),
    /** Distributor 分销商端（预留，复用 organ 账号） */
    DISTRIBUTOR("distributor", "Distributor-Token");

    /** Sa-Token loginType（命名空间标识） */
    private final String loginType;
    /** 对外 Token 请求头名称 */
    private final String tokenName;

    AccountType(String loginType, String tokenName) {
        this.loginType = loginType;
        this.tokenName = tokenName;
    }

    public String getLoginType() {
        return loginType;
    }

    public String getTokenName() {
        return tokenName;
    }
}
