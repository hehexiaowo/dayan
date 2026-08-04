package com.dayan.common.security.config;

import org.springframework.context.annotation.Configuration;

/**
 * Sa-Token 全局配置。
 *
 * <p>四端 Token 命名空间通过 {@code StpKit} 的不同 loginType 实现隔离，
 * 具体 token-name / 有效期 / Redis 前缀等通过 application.yml 的 {@code sa-token.*} 配置：
 *
 * <pre>
 * sa-token:
 *   token-name: Authorization   # 由各端 StpLogic 覆盖（Admin-Token 等）
 *   timeout: 7200               # Admin/Channel 2h
 *   active-timeout: -1
 *   is-concurrent: true
 *   is-share: false
 *   token-style: uuid
 * </pre>
 *
 * <p>多端有效期差异（Admin/Channel 2h、Agent/Client 7d）在登录时通过
 * {@code StpKit.XXX.login(id, new SaLoginModel().setTimeout(...))} 指定，覆盖全局。
 */
@Configuration
public class SaTokenConfig {
    // 全局参数通过 application.yml 配置；多端 StpLogic 通过 StpKit 静态实例提供。
    // 如需自定义 StpInterface（权限/角色查询），在对应业务模块实现并注册 Bean。
}
