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
 */
public final class ContextHolder {

    private static final ThreadLocal<String> CHANNEL_CODE = new ThreadLocal<>();
    private static final ThreadLocal<String> ACCOUNT_CODE = new ThreadLocal<>();
    private static final ThreadLocal<String> ACCOUNT_TYPE = new ThreadLocal<>();
    private static final ThreadLocal<String> ACCOUNT_NAME = new ThreadLocal<>();

    private ContextHolder() {
    }

    public static void setChannelCode(String channelCode) {
        CHANNEL_CODE.set(channelCode);
    }

    /** 当前请求绑定的渠道编码；{@code null} 表示未隔离（如系统域/超管操作） */
    public static String getChannelCode() {
        return CHANNEL_CODE.get();
    }

    public static void setAccountCode(String accountCode) {
        ACCOUNT_CODE.set(accountCode);
    }

    /** 当前登录账号编码，用于自动填充 creator/updater */
    public static String getAccountCode() {
        return ACCOUNT_CODE.get();
    }

    public static void setAccountType(String accountType) {
        ACCOUNT_TYPE.set(accountType);
    }

    /** 账号类型：admin/channel/agent/client/supplier/distributor */
    public static String getAccountType() {
        return ACCOUNT_TYPE.get();
    }

    public static void setAccountName(String accountName) {
        ACCOUNT_NAME.set(accountName);
    }

    /** 当前登录账号姓名（用于操作日志审计展示），未登录或未设置时为 null */
    public static String getAccountName() {
        return ACCOUNT_NAME.get();
    }

    /** 清理全部上下文（请求结束必须调用） */
    public static void clear() {
        CHANNEL_CODE.remove();
        ACCOUNT_CODE.remove();
        ACCOUNT_TYPE.remove();
        ACCOUNT_NAME.remove();
    }

    /** 系统级账号标识（无具体操作人时使用） */
    public static final String SYSTEM_OPERATOR = "system";
}
