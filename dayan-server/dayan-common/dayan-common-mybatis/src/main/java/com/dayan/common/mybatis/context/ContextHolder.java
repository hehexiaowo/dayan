package com.dayan.common.mybatis.context;

/**
 * 请求上下文（ThreadLocal）。
 *
 * <p>存放当前登录用户的渠道编码、账号标识等信息，供：
 * <ul>
 *   <li>{@code DayanTenantHandler} 读取 channel_code 实现 MyBatis-Plus 租户字段级隔离</li>
 *   <li>{@code DayanMetaObjectHandler} 读取账号标识自动填充 creator/updater</li>
 * </ul>
 *
 * <p>由 dayan-common-security 的 {@code SaTokenContextFilter} 在每次请求时设置，
 * 请求结束清理，避免线程池串号。
 *
 * <p>内部使用单一 {@link RequestContext} 对象持有所有字段，
 * {@code clear()} 只需一次 {@code ThreadLocal.remove()}，降低遗漏风险。
 */
public final class ContextHolder {

    private static final ThreadLocal<RequestContext> CONTEXT = new ThreadLocal<>();

    private ContextHolder() {
    }

    /** 获取当前请求上下文（懒创建） */
    private static RequestContext get() {
        RequestContext ctx = CONTEXT.get();
        if (ctx == null) {
            ctx = new RequestContext();
            CONTEXT.set(ctx);
        }
        return ctx;
    }

    public static void setChannelCode(String channelCode) {
        get().setChannelCode(channelCode);
    }

    /** 当前请求绑定的渠道编码；{@code null} 表示未隔离（如系统域/超管操作） */
    public static String getChannelCode() {
        RequestContext ctx = CONTEXT.get();
        return ctx == null ? null : ctx.getChannelCode();
    }

    public static void setAccountCode(String accountCode) {
        get().setAccountCode(accountCode);
    }

    /** 当前登录账号编码，用于自动填充 creator/updater */
    public static String getAccountCode() {
        RequestContext ctx = CONTEXT.get();
        return ctx == null ? null : ctx.getAccountCode();
    }

    public static void setAccountType(String accountType) {
        get().setAccountType(accountType);
    }

    /** 账号类型：admin/channel/agent/client/supplier/distributor */
    public static String getAccountType() {
        RequestContext ctx = CONTEXT.get();
        return ctx == null ? null : ctx.getAccountType();
    }

    public static void setAccountName(String accountName) {
        get().setAccountName(accountName);
    }

    /** 当前登录账号姓名（用于操作日志审计展示），未登录或未设置时为 null */
    public static String getAccountName() {
        RequestContext ctx = CONTEXT.get();
        return ctx == null ? null : ctx.getAccountName();
    }

    /** 清理全部上下文（请求结束必须调用） */
    public static void clear() {
        CONTEXT.remove();
    }

    /** 系统级账号标识（无具体操作人时使用） */
    public static final String SYSTEM_OPERATOR = "system";
}
