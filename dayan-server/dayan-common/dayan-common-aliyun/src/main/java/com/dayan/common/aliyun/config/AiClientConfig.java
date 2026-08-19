package com.dayan.common.aliyun.config;

import com.dayan.common.aliyun.bailian.BailianChatClient;
import com.dayan.common.aliyun.dashscope.DashScopeImageClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * AI 客户端 Bean 注册。
 *
 * <p>{@link BailianChatClient} 和 {@link DashScopeImageClient} 均为无状态 HTTP 客户端
 * （仅持有一个 JDK {@code HttpClient} 实例），适合作为单例 Bean 管理。
 * 凭据（API-Key / 网关域名）在每次方法调用时由调用方传入，不绑定到 Bean 实例。
 */
@Configuration
public class AiClientConfig {

    @Bean
    public BailianChatClient bailianChatClient() {
        return new BailianChatClient();
    }

    @Bean
    public DashScopeImageClient dashScopeImageClient() {
        return new DashScopeImageClient();
    }
}
