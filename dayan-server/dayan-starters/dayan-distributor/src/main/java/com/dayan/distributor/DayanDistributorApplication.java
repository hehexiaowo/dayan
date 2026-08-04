package com.dayan.distributor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * Distributor 分销商端（预留）启动类（端口 8085，context-path /distributor-api）。
 *
 * 扫描说明：业务模块跨包分布，service/mapper 各端共享，扫描整个 com.dayan 树；
 * controller 按「端」分包，用 excludeFilters 排除非本端 controller（详见 admin 端注释）。
 * 本端保留 controller.distributor。
 */
@EnableAsync
@SpringBootApplication
@EnableDiscoveryClient
@ComponentScan(
    basePackages = "com.dayan",
    excludeFilters = {
        @ComponentScan.Filter(
            type = FilterType.REGEX,
            pattern = "com\\.dayan\\.[^.]+\\.controller\\.(admin|agent|channel|client|supplier|open)\\..*"
        )
    }
)
@MapperScan("com.dayan.**.mapper")
public class DayanDistributorApplication {

    public static void main(String[] args) {
        SpringApplication.run(DayanDistributorApplication.class, args);
    }
}
