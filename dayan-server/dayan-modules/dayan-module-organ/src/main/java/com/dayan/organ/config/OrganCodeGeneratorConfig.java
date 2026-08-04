package com.dayan.organ.config;

import cn.dev33.satoken.stp.StpUtil;
import com.dayan.common.core.code.CodeGenerator;
import com.dayan.common.core.code.SequenceProvider;
import com.dayan.common.security.StpKit;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * organ 域基础设施配置。
 *
 * <p>职责：
 * <ol>
 *   <li>暴露 {@link CodeGenerator} 为 Spring Bean（序列号来源为 SequenceProvider / RedisSequenceProvider）</li>
 *   <li>将 {@link StpKit#ADMIN} 注册为 Sa-Token 默认 StpLogic（{@link StpUtil}），
 *       使 {@code @SaCheckPermission("xxx")}（未指定 type 时）按 admin 端命名空间鉴权，
 *       与 Admin 登录（{@code StpKit.ADMIN.login}）一致。</li>
 * </ol>
 *
 * <p>放在 organ 模块内，避免改动 common 模块（common 仅提供工具类，不强制装配）。
 */
@Slf4j
@Configuration
public class OrganCodeGeneratorConfig {

    @Bean
    public CodeGenerator codeGenerator(SequenceProvider sequenceProvider) {
        return new CodeGenerator(sequenceProvider);
    }

    /**
     * 将 ADMIN 端 StpLogic 设为 Sa-Token 默认 StpLogic。
     *
     * <p>未显式指定 type 的 {@code @SaCheckPermission} / {@code @SaCheckRole} 注解会走默认 StpLogic，
     * 需与 Admin 登录命名空间（loginType=admin）保持一致，鉴权才能正确回调 DayanStpInterface。
     */
    @PostConstruct
    public void registerDefaultStpLogic() {
        StpUtil.setStpLogic(StpKit.ADMIN);
        log.info("Sa-Token 默认 StpLogic 已设置为 ADMIN（loginType=admin）");
    }
}

