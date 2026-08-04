package com.dayan.common.security;

import cn.dev33.satoken.stp.StpLogic;

/**
 * 多端 Sa-Token 工具集。
 *
 * <p>四端各自独立 StpLogic 实例，loginType 不同实现命名空间隔离：
 * <ul>
 *   <li>{@link #ADMIN} - Admin 运营端</li>
 *   <li>{@link #CHANNEL} - Channel 渠道端</li>
 *   <li>{@link #AGENT} - Agent 代理人端</li>
 *   <li>{@link #CLIENT} - Client 客户端</li>
 *   <li>{@link #SUPPLIER} - Supplier 供应商端（预留）</li>
 *   <li>{@link #DISTRIBUTOR} - Distributor 分销商端（预留）</li>
 * </ul>
 *
 * <p>每端 Token 存储在独立 Redis key 空间，互不串扰。
 */
public final class StpKit {

    private StpKit() {
    }

    public static final StpLogic ADMIN = new StpLogic(AccountType.ADMIN.getLoginType());
    public static final StpLogic CHANNEL = new StpLogic(AccountType.CHANNEL.getLoginType());
    public static final StpLogic AGENT = new StpLogic(AccountType.AGENT.getLoginType());
    public static final StpLogic CLIENT = new StpLogic(AccountType.CLIENT.getLoginType());
    public static final StpLogic SUPPLIER = new StpLogic(AccountType.SUPPLIER.getLoginType());
    public static final StpLogic DISTRIBUTOR = new StpLogic(AccountType.DISTRIBUTOR.getLoginType());

    /**
     * 按 loginType 获取对应端的 StpLogic。
     */
    public static StpLogic of(String loginType) {
        switch (loginType) {
            case "admin": return ADMIN;
            case "channel": return CHANNEL;
            case "agent": return AGENT;
            case "client": return CLIENT;
            case "supplier": return SUPPLIER;
            case "distributor": return DISTRIBUTOR;
            default:
                throw new IllegalArgumentException("未知 loginType: " + loginType);
        }
    }
}