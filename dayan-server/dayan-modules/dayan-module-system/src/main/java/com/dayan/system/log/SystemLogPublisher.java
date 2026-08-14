package com.dayan.system.log;

import com.dayan.common.log.operation.OperationLogRecord;
import com.dayan.common.log.operation.OperationLogPublisher;
import com.dayan.system.entity.SystemLogEntry;
import com.dayan.system.enums.SystemLogSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 操作日志落库实现（system 域）。
 *
 * <p>实现 common-log 的 {@link OperationLogPublisher} 接口，
 * 被 {@code OperationLogAspect} 自动发现并调用。
 * 按 record.accountType 经 {@link SystemLogRouter} 路由到对应端的 system_log_* 表；
 * accountType 为空/系统调用兜底落入 system_log_organ。
 *
 * <p>注意：{@code convert} 在主请求线程完成字段拷贝（含 record 中已解析好的操作人），
 * 仅 {@code router.save} 为异步，避免 ThreadLocal 跨线程丢失。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SystemLogPublisher implements OperationLogPublisher {

    private final SystemLogRouter router;

    @Override
    public void publish(OperationLogRecord record) {
        try {
            SystemLogEntry entry = convert(record);
            router.save(entry);
        } catch (Exception e) {
            log.warn("操作日志落库失败: traceId={}, module={}, err={}",
                    record.getTraceId(), record.getModule(), e.getMessage());
        }
    }

    private SystemLogEntry convert(OperationLogRecord record) {
        SystemLogSource source = SystemLogSource.fromAccountType(record.getAccountType());
        SystemLogEntry entry = router.newEntry(source);
        entry.setTraceId(record.getTraceId());
        entry.setModule(record.getModule());
        entry.setAction(record.getAction());
        // account_code / account_type / account_name：由切面在主线程经 OperatorResolver 填入 record
        // NOT NULL 列兜底 "unknown" / "system"
        String accountCode = record.getOperator();
        entry.setAccountCode(accountCode != null && !accountCode.isEmpty() ? accountCode : "unknown");
        String accountType = record.getAccountType();
        entry.setAccountType(accountType != null && !accountType.isEmpty() ? accountType : "system");
        entry.setAccountName(record.getAccountName());
        entry.setRequestUrl(record.getUri());
        entry.setRequestMethod(record.getHttpMethod());
        entry.setRequestParams(truncate(record.getArgs(), 2000));
        entry.setResponseResult(truncate(record.getResult(), 4000));
        entry.setResponseCode(record.isSuccess() ? 0 : 1);
        entry.setResultStatus(record.isSuccess() ? 1 : 0);
        entry.setErrorMsg(truncate(record.getErrorMsg(), 500));
        entry.setDuration((int) record.getCostMs());
        entry.setIpAddress(record.getIp());
        entry.setIpLocation(record.getIpLocation());
        entry.setUserAgent(record.getUserAgent());
        entry.setDeviceType(record.getDeviceType());
        entry.setOs(record.getOs());
        entry.setBrowser(record.getBrowser());
        return entry;
    }

    private String truncate(String s, int max) {
        if (s == null) {
            return null;
        }
        return s.length() <= max ? s : s.substring(0, max);
    }
}
