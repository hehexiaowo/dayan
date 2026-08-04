package com.dayan.agent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * Agent 代理人端启动类（端口 8082，context-path /agent-api）。
 */
@EnableAsync
@SpringBootApplication
@EnableDiscoveryClient
public class DayanAgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(DayanAgentApplication.class, args);
    }
}
