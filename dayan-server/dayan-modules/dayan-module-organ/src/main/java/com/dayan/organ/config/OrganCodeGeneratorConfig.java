package com.dayan.organ.config;

import com.dayan.common.core.code.CodeGenerator;
import com.dayan.common.core.code.SequenceProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * organ 域编码生成器配置。
 *
 * <p>将 {@link CodeGenerator} 暴露为 Spring Bean，供 organ 域内角色/权限等业务编码生成使用。
 * 序列号来源为 {@link SequenceProvider}（生产环境为 RedisSequenceProvider）。
 *
 * <p>放在 organ 模块内，避免改动 common 模块（common 仅提供工具类，不强制装配）。
 */
@Configuration
public class OrganCodeGeneratorConfig {

    @Bean
    public CodeGenerator codeGenerator(SequenceProvider sequenceProvider) {
        return new CodeGenerator(sequenceProvider);
    }
}
