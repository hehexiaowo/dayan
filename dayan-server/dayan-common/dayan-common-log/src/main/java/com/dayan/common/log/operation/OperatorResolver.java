package com.dayan.common.log.operation;

/**
 * 操作人信息解析器（SPI）。
 *
 * <p>common-log 不依赖具体的安全/上下文模块，由上层模块（如 common-security）
 * 实现本接口，把当前登录账号的编码/姓名/类型注入到操作日志切面中。
 *
 * <p>切面在主请求线程调用 {@link #resolveCode()} 等方法，把结果写入
 * {@link OperationLogRecord}，随后异步落库时直接从 record 取值，
 * 避免 @Async 线程拿不到 ThreadLocal 的问题。
 */
public interface OperatorResolver {

    /** 当前登录账号编码；未登录时返回 null */
    String resolveCode();

    /** 当前登录账号姓名；未登录或未设置时返回 null */
    String resolveName();

    /** 当前登录账号类型（admin/channel/agent 等）；未登录时返回 null */
    String resolveType();
}
