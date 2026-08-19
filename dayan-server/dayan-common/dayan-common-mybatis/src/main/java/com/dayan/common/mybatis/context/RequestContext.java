package com.dayan.common.mybatis.context;

import lombok.Data;

/**
 * 请求上下文数据对象。
 *
 * <p>聚合当前请求的用户信息，由 {@link ContextHolder} 以 {@code ThreadLocal} 形式持有。
 * 合并为单一对象后，{@code clear()} 只需一次 {@code ThreadLocal.remove()}，降低遗漏风险。
 */
@Data
public class RequestContext {

    /** 当前请求绑定的渠道编码；{@code null} 表示未隔离（如系统域/超管操作） */
    private String channelCode;

    /** 当前登录账号编码，用于自动填充 creator/updater */
    private String accountCode;

    /** 账号类型：admin/channel/agent/client/supplier/distributor */
    private String accountType;

    /** 当前登录账号姓名（用于操作日志审计展示），未登录或未设置时为 null */
    private String accountName;
}
