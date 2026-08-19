package com.dayan.common.aliyun.bailian;

import com.dayan.common.core.exception.BusinessException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link BailianChatClient} 纯逻辑测试（不启动 Spring 上下文）。
 */
class BailianChatClientTest {

    private final BailianChatClient client = new BailianChatClient();

    // ==================== validateAndBuildUrl（通过 chat 方法间接测试） ====================

    @Test
    void chat_apiKey空白_抛业务异常() {
        assertThatThrownBy(() -> client.chat("", "https://api.example.com", "qwen-plus",
                List.of(new BailianChatClient.Message("user", "hi")), 0.3))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("API-Key 未配置");
    }

    @Test
    void chat_apiKey为null_抛业务异常() {
        assertThatThrownBy(() -> client.chat(null, "https://api.example.com", "qwen-plus",
                List.of(new BailianChatClient.Message("user", "hi")), 0.3))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("API-Key 未配置");
    }

    @Test
    void chat_apiHost空白_抛业务异常() {
        assertThatThrownBy(() -> client.chat("sk-test", "", "qwen-plus",
                List.of(new BailianChatClient.Message("user", "hi")), 0.3))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("网关域名未配置");
    }

    @Test
    void chat_apiHost为null_抛业务异常() {
        assertThatThrownBy(() -> client.chat("sk-test", null, "qwen-plus",
                List.of(new BailianChatClient.Message("user", "hi")), 0.3))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("网关域名未配置");
    }

    // ==================== Message record ====================

    @Test
    void message_record_属性正确() {
        BailianChatClient.Message msg = new BailianChatClient.Message("user", "hello");
        assertThat(msg.role()).isEqualTo("user");
        assertThat(msg.content()).isEqualTo("hello");
    }

    // ==================== DeltaListener 接口 ====================

    @Test
    void deltaListener_functionalInterface() {
        StringBuilder sb = new StringBuilder();
        BailianChatClient.DeltaListener listener = sb::append;
        listener.onDelta("hello");
        listener.onDelta(" world");
        assertThat(sb.toString()).isEqualTo("hello world");
    }
}
