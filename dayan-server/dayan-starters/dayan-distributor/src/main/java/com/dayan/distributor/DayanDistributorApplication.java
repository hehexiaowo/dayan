package com.dayan.distributor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * Distributor 分销商端（预留）启动类（端口 8085，context-path /distributor-api）。
 */
@EnableAsync
@SpringBootApplication
@EnableDiscoveryClient
public class DayanDistributorApplication {

    public static void main(String[] args) {
        SpringApplication.run(DayanDistributorApplication.class, args);
    }
}
