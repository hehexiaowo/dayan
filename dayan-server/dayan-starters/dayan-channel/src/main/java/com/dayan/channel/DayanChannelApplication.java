package com.dayan.channel;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * Channel 渠道端启动类（端口 8081，context-path /channel-api）。
 */
@EnableAsync
@SpringBootApplication
@EnableDiscoveryClient
public class DayanChannelApplication {

    public static void main(String[] args) {
        SpringApplication.run(DayanChannelApplication.class, args);
    }
}
