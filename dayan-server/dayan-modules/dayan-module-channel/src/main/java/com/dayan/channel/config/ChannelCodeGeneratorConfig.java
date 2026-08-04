package com.dayan.channel.config;

import com.dayan.common.core.code.CodeGenerator;
import com.dayan.common.core.code.SequenceProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 渠道域基础设施配置。
 *
 * <p>暴露 {@link CodeGenerator} 为 Spring Bean（仅在容器中尚不存在时生效，避免与
 * dayan-module-organ 的 {@code OrganCodeGeneratorConfig} 重复装配冲突）。
 */
@Slf4j
@Configuration
public class ChannelCodeGeneratorConfig {

    @Bean
    @ConditionalOnMissingBean(CodeGenerator.class)
    public CodeGenerator codeGenerator(SequenceProvider sequenceProvider) {
        log.info("渠道域装配 CodeGenerator（容器中此前不存在，新建）");
        return new CodeGenerator(sequenceProvider);
    }
}
