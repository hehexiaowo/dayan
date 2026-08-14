package com.dayan.system.enums;

/**
 * 系统日志来源（四端分表）。
 *
 * <p>对应表：organ → system_log_organ，channel → system_log_channel，
 * agent → system_log_agent，client → system_log_client。
 */
public enum SystemLogSource {

    /** 管理后台（admin 端；supplier/distributor/system/unknown 均无独立端，兜底落此表） */
    ORGAN,
    /** 渠道端 */
    CHANNEL,
    /** 代理人端 */
    AGENT,
    /** 客户端 */
    CLIENT;

    /**
     * 按账号类型（Sa-Token loginType）路由到日志来源。
     * admin/supplier/distributor/system/unknown/null 一律归 ORGAN。
     */
    public static SystemLogSource fromAccountType(String accountType) {
        if (accountType == null || accountType.isBlank()) {
            return ORGAN;
        }
        return switch (accountType) {
            case "channel" -> CHANNEL;
            case "agent" -> AGENT;
            case "client" -> CLIENT;
            default -> ORGAN;
        };
    }

    /** 按查询参数解析（大小写不敏感，非法值兜底 ORGAN） */
    public static SystemLogSource of(String source) {
        if (source == null || source.isBlank()) {
            return ORGAN;
        }
        try {
            return valueOf(source.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return ORGAN;
        }
    }
}
