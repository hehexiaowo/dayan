package com.dayan.admin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * Admin 运营端启动类（端口 8080，context-path /admin-api）。
 */
@EnableAsync
@SpringBootApplication
@EnableDiscoveryClient
public class DayanAdminApplication {

    public static void main(String[] args) {
        SpringApplication.run(DayanAdminApplication.class, args);
    }
}
