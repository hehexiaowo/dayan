package com.dayan.admin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * Admin 运营端启动类（端口 8080，context-path /admin-api）。
 *
 * 扫描说明（关键）：
 * - 业务模块跨包分布（com.dayan.organ / com.dayan.park / com.dayan.scene ...），
 *   service/mapper/entity 各端共享，故 ComponentScan 扫描整个 com.dayan 树；
 * - 但 controller 按「端」分包：com.dayan.{module}.controller.{end}，
 *   各端 controller 的 @RequestMapping 路径相同（靠 context-path 区分），若被同一上下文
 *   加载会触发 Ambiguous mapping。故用 excludeFilters 排除非本端（agent/channel/client/
 *   distributor/supplier/open）的 controller 包，仅保留 controller.admin。
 *   新增「端」时只需在该正则的端名列表里补一个名字，无需改其他代码。
 * - @MapperScan("com.dayan.**.mapper")：MyBatis-Plus Mapper 接口同样跨模块分布，全量扫描无冲突。
 */
@EnableAsync
@SpringBootApplication
@EnableDiscoveryClient
@ComponentScan(
    basePackages = "com.dayan",
    excludeFilters = {
        @ComponentScan.Filter(
            type = FilterType.REGEX,
            // 匹配 com.dayan.{任意模块}.controller.{非admin端}.{任意类}
            // controller.admin 不在此列，故 admin 端 controller 全部保留
            pattern = "com\\.dayan\\.[^.]+\\.controller\\.(agent|channel|client|distributor|supplier|open)\\..*"
        )
    }
)
@MapperScan("com.dayan.**.mapper")
public class DayanAdminApplication {

    public static void main(String[] args) {
        SpringApplication.run(DayanAdminApplication.class, args);
    }
}
