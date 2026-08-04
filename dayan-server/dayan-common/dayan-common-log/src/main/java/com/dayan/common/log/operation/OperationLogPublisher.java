package com.dayan.common.log.operation;

/**
 * 操作日志发布者接口。
 *
 * <p>由 system 业务模块实现（异步落库 system_operation_log）。
 * common-log 不直接依赖业务模块，仅声明接口，业务模块实现并注册 Bean。
 */
public interface OperationLogPublisher {

    /**
     * 异步发布操作日志。
     */
    void publish(OperationLogRecord record);
}
