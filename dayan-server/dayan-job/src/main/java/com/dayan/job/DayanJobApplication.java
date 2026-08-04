package com.dayan.job;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 定时任务模块启动类。
 *
 * <p>独立部署，不对外提供 HTTP 接口，仅注册到 Nacos 便于服务发现。
 * 通过 Spring {@code @Scheduled} 执行定时任务（P0 占位，P4/P7 补真实逻辑）。
 * 生产环境可切换为 XXL-Job 调度中心。
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableScheduling
public class DayanJobApplication {

    public static void main(String[] args) {
        SpringApplication.run(DayanJobApplication.class, args);
    }
}
