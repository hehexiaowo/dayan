package com.dayan.job;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 定时任务模块启动类。
 *
 * <p>独立部署，不对外提供 HTTP 接口，仅注册到 Nacos 便于服务发现。
 * 通过 Spring {@code @Scheduled} 执行定时任务：
 * <ul>
 *   <li>权益过期扫描（P4 接入 EquityDepotService + EQUITY_SM 状态机）</li>
 *   <li>订单超时取消（P7）</li>
 *   <li>自动对账（P7）</li>
 * </ul>
 * 生产环境可切换为 XXL-Job 调度中心。
 *
 * <p>扫描范围扩展至 {@code com.dayan.equity}，以便注入权益域 Mapper/Service。
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableScheduling
@ComponentScan(basePackages = {"com.dayan.job", "com.dayan.equity", "com.dayan.common"})
@MapperScan(basePackages = {"com.dayan.job.mapper", "com.dayan.equity.mapper"})
public class DayanJobApplication {

    public static void main(String[] args) {
        SpringApplication.run(DayanJobApplication.class, args);
    }
}
