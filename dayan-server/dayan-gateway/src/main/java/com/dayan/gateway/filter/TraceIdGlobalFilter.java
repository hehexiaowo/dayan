package com.dayan.gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * 网关全局过滤器：生成/透传 traceId。
 *
 * <p>对每个进入网关的请求，若无 X-Trace-Id 则生成，写入请求头透传给下游服务，
 * 同时写入响应头便于客户端关联日志。
 */
@Component
public class TraceIdGlobalFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(TraceIdGlobalFilter.class);
    public static final String TRACE_ID_HEADER = "X-Trace-Id";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String traceId = exchange.getRequest().getHeaders().getFirst(TRACE_ID_HEADER);
        if (!StringUtils.hasText(traceId)) {
            traceId = generateTraceId();
        }
        final String finalTraceId = traceId;

        ServerHttpRequest request = exchange.getRequest().mutate()
                .header(TRACE_ID_HEADER, finalTraceId)
                .build();
        final ServerWebExchange finalExchange = exchange.mutate().request(request).build();
        finalExchange.getResponse().getHeaders().add(TRACE_ID_HEADER, finalTraceId);

        long start = System.currentTimeMillis();
        return chain.filter(finalExchange).doFinally(signal -> {
            long cost = System.currentTimeMillis() - start;
            log.info("[{}] {} {} cost={}ms",
                    finalTraceId,
                    finalExchange.getRequest().getMethod(),
                    finalExchange.getRequest().getURI().getPath(),
                    cost);
        });
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    private String generateTraceId() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
