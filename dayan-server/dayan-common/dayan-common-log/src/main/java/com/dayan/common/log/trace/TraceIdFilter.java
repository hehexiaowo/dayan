package com.dayan.common.log.trace;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * 链路追踪 ID 过滤器。
 *
 * <p>每个请求生成/透传 traceId，写入 MDC（供 logback 的 %X{traceId} 输出）+ 响应头 X-Trace-Id。
 * 优先沿用上游传入的 X-Trace-Id（网关/调用方），无则生成。
 *
 * <p>过滤器顺序最高（最早执行），确保后续所有日志（含业务异常）都带 traceId。
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceIdFilter extends OncePerRequestFilter {

    public static final String TRACE_ID_KEY = "traceId";
    public static final String TRACE_ID_HEADER = "X-Trace-Id";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String traceId = request.getHeader(TRACE_ID_HEADER);
        if (!StringUtils.hasText(traceId)) {
            traceId = generateTraceId();
        }
        MDC.put(TRACE_ID_KEY, traceId);
        response.setHeader(TRACE_ID_HEADER, traceId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(TRACE_ID_KEY);
        }
    }

    private String generateTraceId() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
