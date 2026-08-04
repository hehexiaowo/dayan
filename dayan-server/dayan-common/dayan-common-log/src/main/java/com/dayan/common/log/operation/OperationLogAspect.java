package com.dayan.common.log.operation;

import com.dayan.common.log.sensitive.SensitiveUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 操作日志 AOP 切面。
 *
 * <p>拦截标注 {@link OperationLog} 的方法，构造 {@link OperationLogRecord} 后：
 * <ol>
 *   <li>记录到 slf4j（info 级别，P0 兜底，确保即使无 Publisher 也有日志）</li>
 *   <li>委托 {@link OperationLogPublisher} 异步落库（业务模块提供时启用）</li>
 * </ol>
 */
@Aspect
@Component
public class OperationLogAspect {

    private static final Logger log = LoggerFactory.getLogger(OperationLogAspect.class);

    private final ObjectMapper objectMapper;
    private final ObjectProvider<OperationLogPublisher> publisherProvider;

    public OperationLogAspect(ObjectMapper objectMapper,
                              ObjectProvider<OperationLogPublisher> publisherProvider) {
        this.objectMapper = objectMapper;
        this.publisherProvider = publisherProvider;
    }

    @Around("@annotation(operationLog)")
    public Object around(ProceedingJoinPoint joinPoint, OperationLog operationLog) throws Throwable {
        long start = System.currentTimeMillis();
        OperationLogRecord.OperationLogRecordBuilder builder = OperationLogRecord.builder()
                .module(operationLog.module())
                .action(operationLog.action())
                .traceId(MDC.get("traceId"));

        fillRequest(builder);

        if (operationLog.logArgs()) {
            builder.args(serializeArgs(joinPoint, operationLog));
        }

        Object result = null;
        try {
            result = joinPoint.proceed();
            builder.success(true);
            if (operationLog.logResult()) {
                builder.result(safeToJson(result));
            }
            return result;
        } catch (Throwable ex) {
            builder.success(false);
            builder.errorMsg(ex.getMessage());
            throw ex;
        } finally {
            builder.costMs(System.currentTimeMillis() - start);
            OperationLogRecord record = builder.build();
            log.info("操作日志: module={}, action={}, operator={}, uri={}, success={}, cost={}ms",
                    record.getModule(), record.getAction(), record.getOperator(),
                    record.getUri(), record.isSuccess(), record.getCostMs());
            OperationLogPublisher publisher = publisherProvider.getIfAvailable();
            if (publisher != null) {
                publisher.publish(record);
            }
        }
    }

    private void fillRequest(OperationLogRecord.OperationLogRecordBuilder builder) {
        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs != null) {
            HttpServletRequest request = attrs.getRequest();
            builder.httpMethod(request.getMethod());
            builder.uri(request.getRequestURI());
        }
    }

    private String serializeArgs(ProceedingJoinPoint joinPoint, OperationLog operationLog) {
        try {
            MethodSignature sig = (MethodSignature) joinPoint.getSignature();
            String[] paramNames = sig.getParameterNames();
            Object[] args = joinPoint.getArgs();
            StringBuilder sb = new StringBuilder("{");
            for (int i = 0; i < args.length; i++) {
                if (i > 0) sb.append(",");
                String name = paramNames != null && i < paramNames.length ? paramNames[i] : "arg" + i;
                String value = safeToJson(args[i]);
                if (shouldMask(operationLog, name)) {
                    value = SensitiveUtil.maskJson(value);
                }
                sb.append("\"").append(name).append("\":").append(value);
            }
            return sb.append("}").toString();
        } catch (Exception e) {
            return "[serialize-failed]";
        }
    }

    private boolean shouldMask(OperationLog operationLog, String paramName) {
        String mask = operationLog.maskFields();
        if (mask == null || mask.isEmpty()) {
            return false;
        }
        for (String f : mask.split(",")) {
            if (f.trim().equalsIgnoreCase(paramName)) {
                return true;
            }
        }
        return false;
    }

    private String safeToJson(Object obj) {
        if (obj == null) return "null";
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return String.valueOf(obj);
        }
    }
}
