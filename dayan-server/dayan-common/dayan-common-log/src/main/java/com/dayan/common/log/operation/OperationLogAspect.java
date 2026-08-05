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
    private final ObjectProvider<OperatorResolver> operatorResolverProvider;

    public OperationLogAspect(ObjectMapper objectMapper,
                              ObjectProvider<OperationLogPublisher> publisherProvider,
                              ObjectProvider<OperatorResolver> operatorResolverProvider) {
        this.objectMapper = objectMapper;
        this.publisherProvider = publisherProvider;
        this.operatorResolverProvider = operatorResolverProvider;
    }

    @Around("@annotation(operationLog)")
    public Object around(ProceedingJoinPoint joinPoint, OperationLog operationLog) throws Throwable {
        long start = System.currentTimeMillis();
        OperationLogRecord.OperationLogRecordBuilder builder = OperationLogRecord.builder()
                .module(operationLog.module())
                .action(operationLog.action())
                .traceId(MDC.get("traceId"));

        // 在主请求线程解析操作人信息（异步落库时 ThreadLocal 已失效）
        fillOperator(builder);
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
            parseUserAgent(record);
            log.info("操作日志: module={}, action={}, operator={}, uri={}, success={}, cost={}ms",
                    record.getModule(), record.getAction(), record.getOperator(),
                    record.getUri(), record.isSuccess(), record.getCostMs());
            OperationLogPublisher publisher = publisherProvider.getIfAvailable();
            if (publisher != null) {
                publisher.publish(record);
            }
        }
    }

    /**
     * 按 User-Agent 简单解析设备类型/操作系统/浏览器。
     * 仅覆盖主流 UA，边缘 UA 解析失败时字段留 null（不影响主流程）。
     */
    private void parseUserAgent(OperationLogRecord record) {
        String ua = record.getUserAgent();
        if (ua == null || ua.isEmpty()) {
            return;
        }
        String uaLower = ua.toLowerCase();
        // 设备类型
        if (uaLower.contains("mobile") || uaLower.contains("android") || uaLower.contains("iphone")) {
            if (uaLower.contains("tablet") || uaLower.contains("ipad")) {
                record.setDeviceType("tablet");
            } else {
                record.setDeviceType("mobile");
            }
        } else {
            record.setDeviceType("pc");
        }
        // 操作系统
        if (uaLower.contains("windows")) {
            record.setOs("Windows");
        } else if (uaLower.contains("mac os") || uaLower.contains("macintosh")) {
            record.setOs("macOS");
        } else if (uaLower.contains("linux")) {
            record.setOs("Linux");
        } else if (uaLower.contains("android")) {
            record.setOs("Android");
        } else if (uaLower.contains("iphone") || uaLower.contains("ipad") || uaLower.contains("ios")) {
            record.setOs("iOS");
        }
        // 浏览器（注意顺序：Edge/Edg 要先于 Chrome 判断，否则会被 Chrome 吞掉）
        if (uaLower.contains("edg/") || uaLower.contains("edge")) {
            record.setBrowser("Edge");
        } else if (uaLower.contains("firefox")) {
            record.setBrowser("Firefox");
        } else if (uaLower.contains("chrome")) {
            record.setBrowser("Chrome");
        } else if (uaLower.contains("safari")) {
            record.setBrowser("Safari");
        }
    }

    /**
     * 在主请求线程解析当前操作人信息（账号编码/姓名/类型）。
     *
     * <p>必须在此处（主线程）解析，因为后续 {@code publisher.publish} 是 @Async 异步执行，
     * 异步线程拿不到主线程的 ThreadLocal 上下文。
     * 解析结果写入 record，Publisher 落库时直接从 record 取值。
     */
    private void fillOperator(OperationLogRecord.OperationLogRecordBuilder builder) {
        OperatorResolver resolver = operatorResolverProvider.getIfAvailable();
        if (resolver == null) {
            return;
        }
        builder.operator(resolver.resolveCode());
        builder.accountName(resolver.resolveName());
        builder.accountType(resolver.resolveType());
    }

    private void fillRequest(OperationLogRecord.OperationLogRecordBuilder builder) {
        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs != null) {
            HttpServletRequest request = attrs.getRequest();
            builder.httpMethod(request.getMethod());
            builder.uri(request.getRequestURI());
            builder.ip(extractIp(request));
            builder.userAgent(request.getHeader("User-Agent"));
        }
    }

    /**
     * 提取客户端真实 IP：依次尝试 X-Forwarded-For / X-Real-IP / Proxy-Client-IP，
     * 取第一个非 unknown 的值；兜底 request.getRemoteAddr()。
     */
    private String extractIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (isUnknownIp(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (isUnknownIp(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (isUnknownIp(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (isUnknownIp(ip)) {
            ip = request.getRemoteAddr();
        }
        // X-Forwarded-For 可能含多级代理，取第一个
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    private boolean isUnknownIp(String ip) {
        return ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip);
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
