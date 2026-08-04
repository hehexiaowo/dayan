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
    /** 操作人（账号编码） */
    private String operator;
    /** 请求方法 */
    private String httpMethod;
    /** 请求 URI */
    private String uri;
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
