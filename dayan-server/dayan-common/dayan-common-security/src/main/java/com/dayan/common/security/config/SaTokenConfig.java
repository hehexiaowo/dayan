package com.dayan.common.security.config;

import cn.dev33.satoken.interceptor.SaInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import jakarta.annotation.Resource;

/**
 * Sa-Token 拦截器注册。
 *
 * <p>Sa-Token 的 {@code sa-token-spring-boot3-starter} 不会自动把
 * {@link SaInterceptor} 加入 Spring MVC 拦截器链，必须显式注册。
 * 否则 Controller 上的 {@code @SaCheckPermission} / {@code @SaCheckRole} /
 * {@code @SaCheckLogin} 注解不会被触发，导致鉴权形同虚设（P0 级安全漏洞）。
 *
 * <p>本配置注册一个 {@link SaInterceptor}，拦截所有路径，但
 * <b>不附加任何 {@code setAuth} 登录强制校验</b> —— 仅让注解处理器工作。
 * 这样：
 * <ul>
 *   <li>带 {@code @SaCheckPermission} 的方法按注解细粒度鉴权（admin 端 145 处）</li>
 *   <li>无注解的方法（如 agent/channel/client 端 controller，以及 auth/login）不受影响</li>
 * </ul>
 *
 * <p>白名单（登录、API 文档、静态资源）放行拦截器但保留 {@code @SaCheckPermission} 检查能力，
 * 实际上这些路径本来就没有注解，不会被拦。
 */
@Configuration
public class SaTokenConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 注册 Sa-Token 拦截器，使 @SaCheckPermission/@SaCheckRole/@SaCheckLogin 注解生效
        // 不调用 setAuth() —— 不做全局登录强制校验，只让注解处理器工作
        registry.addInterceptor(new SaInterceptor())
                .addPathPatterns("/**")
                .excludePathPatterns(
                        // 认证相关（登录、登出、验证码、微信、渠道查询）
                        "/auth/login",
                        "/auth/logout",
                        "/auth/channels",
                        "/auth/sms/send",
                        "/auth/sms/login",
                        "/auth/wx/login",
                        // API 文档（Knife4j / Springdoc / Swagger）
                        "/doc.html",
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/v3/api-docs/**",
                        "/webjars/**",
                        "/favicon.ico",
                        // Spring Boot Actuator 健康检查
                        "/actuator/**"
                );
    }
}
