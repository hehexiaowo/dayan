package com.dayan.supplier;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * Supplier 供应商端（预留）启动类（端口 8084，context-path /supplier-api）。
 */
@SpringBootApplication
@EnableDiscoveryClient
public class DayanSupplierApplication {

    public static void main(String[] args) {
        SpringApplication.run(DayanSupplierApplication.class, args);
    }
}
