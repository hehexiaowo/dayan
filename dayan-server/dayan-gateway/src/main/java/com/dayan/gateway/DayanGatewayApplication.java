package com.dayan.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * 大雁养老 API 网关启动类。
 *
 * <p>端口 8000，职责：
 * <ul>
 *   <li>六端路由转发（/admin-api/** → dayan-admin 等）</li>
 *   <li>统一 Token 解析与透传（不强制鉴权，交下游）</li>
 *   <li>traceId 透传</li>
 *   <li>限流（Sentinel，后续接入）</li>
 * </ul>
 */
@SpringBootApplication
@EnableDiscoveryClient
public class DayanGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(DayanGatewayApplication.class, args);
    }
}
