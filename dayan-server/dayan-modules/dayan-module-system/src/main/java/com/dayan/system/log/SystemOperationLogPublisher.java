package com.dayan.system.log;

import com.dayan.common.log.operation.OperationLogRecord;
import com.dayan.system.entity.SystemOperationLog;
import com.dayan.system.mapper.SystemOperationLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 操作日志落库实现（system 域）。
 *
 * <p>实现 common-log 的 {@link com.dayan.common.log.operation.OperationLogPublisher} 接口，
 * 被 {@code OperationLogAspect} 自动发现并调用，异步写入 {@code system_operation_log} 表。
 *
 * <p>需在启动模块启用 @EnableAsync（dayan-admin 等启动类补 @EnableAsync）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SystemOperationLogPublisher implements com.dayan.common.log.operation.OperationLogPublisher {

    private final SystemOperationLogMapper operationLogMapper;

    @Async
    @Override
    public void publish(OperationLogRecord record) {
        try {
            SystemOperationLog entity = convert(record);
            operationLogMapper.insert(entity);
        } catch (Exception e) {
            // 落库失败不影响主流程，仅记录日志
            log.warn("操作日志落库失败: traceId={}, module={}, err={}",
                    record.getTraceId(), record.getModule(), e.getMessage());
        }
    }

    private SystemOperationLog convert(OperationLogRecord record) {
        SystemOperationLog entity = new SystemOperationLog();
        entity.setTraceId(record.getTraceId());
        entity.setModule(record.getModule());
        entity.setAction(record.getAction());
        // account_code / account_type / account_name：由切面在主线程经 OperatorResolver 填入 record，
        // 此处（异步线程）直接从 record 取，避免 ThreadLocal 跨线程丢失。
        // NOT NULL 列兜底 "unknown" / "system"
        String accountCode = record.getOperator();
        entity.setAccountCode(accountCode != null && !accountCode.isEmpty() ? accountCode : "unknown");
        String accountType = record.getAccountType();
        entity.setAccountType(accountType != null && !accountType.isEmpty() ? accountType : "system");
        entity.setAccountName(record.getAccountName());
        entity.setRequestUrl(record.getUri());
        entity.setRequestMethod(record.getHttpMethod());
        entity.setRequestParams(truncate(record.getArgs(), 2000));
        entity.setResponseCode(record.isSuccess() ? 0 : 1);
        entity.setResultStatus(record.isSuccess() ? 1 : 0);
        entity.setErrorMsg(truncate(record.getErrorMsg(), 500));
        entity.setDuration((int) record.getCostMs());
        // 终端信息（由 OperationLogAspect 从 HttpServletRequest 采集）
        entity.setIpAddress(record.getIp());
        entity.setIpLocation(record.getIpLocation());
        entity.setUserAgent(truncate(record.getUserAgent(), 500));
        entity.setDeviceType(record.getDeviceType());
        entity.setOs(record.getOs());
        entity.setBrowser(record.getBrowser());
        return entity;
    }

    private String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() > max ? s.substring(0, max) : s;
    }
}
