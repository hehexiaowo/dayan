package com.dayan.client;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * Client 客户端启动类（端口 8083，context-path /client-api）。
 */
@EnableAsync
@SpringBootApplication
@EnableDiscoveryClient
public class DayanClientApplication {

    public static void main(String[] args) {
        SpringApplication.run(DayanClientApplication.class, args);
    }
}
