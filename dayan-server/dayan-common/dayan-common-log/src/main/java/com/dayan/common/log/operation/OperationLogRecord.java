package com.dayan.common.log.operation;

import lombok.Builder;
import lombok.Data;

/**
 * 操作日志记录（传输对象）。
 *
 * <p>由 {@link OperationLogAspect} 构造，交给 {@link OperationLogPublisher} 落库。
 * 业务模块实现 {@link OperationLogPublisher}（异步写入 system_operation_log）。
 */
@Data
@Builder
public class OperationLogRecord {

    /** 操作模块 */
    private String module;
    /** 操作类型 */
    private String action;
    /** 操作人（账号编码，由切面在主线程经 OperatorResolver 填充） */
    private String operator;
    /** 操作人姓名（由切面在主线程填充，避免异步线程丢失 ThreadLocal） */
    private String accountName;
    /** 账号类型（admin/channel/agent 等） */
    private String accountType;
    /** 请求方法 */
    private String httpMethod;
    /** 请求 URI */
    private String uri;
    /** 客户端 IP（经代理透传处理） */
    private String ip;
    /** IP 归属地（需 IP 库解析，暂留空） */
    private String ipLocation;
    /** 浏览器 User-Agent 原文 */
    private String userAgent;
    /** 设备类型（pc/mobile/tablet，按 UA 解析） */
    private String deviceType;
    /** 操作系统（按 UA 解析） */
    private String os;
    /** 浏览器（按 UA 解析） */
    private String browser;
    /** 请求参数（JSON，已脱敏） */
    private String args;
    /** 返回结果（JSON） */
    private String result;
    /** 是否成功 */
    private boolean success;
    /** 异常信息 */
    private String errorMsg;
    /** 耗时（毫秒） */
    private long costMs;
    /** 链路追踪 ID */
    private String traceId;
}
