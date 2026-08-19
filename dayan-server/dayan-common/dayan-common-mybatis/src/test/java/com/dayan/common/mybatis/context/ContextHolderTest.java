package com.dayan.common.mybatis.context;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link ContextHolder} 单元测试（基于 {@link RequestContext} 的单 ThreadLocal 实现）。
 */
class ContextHolderTest {

    @AfterEach
    void cleanup() {
        ContextHolder.clear();
    }

    @Test
    void 初始状态_所有字段为null() {
        assertThat(ContextHolder.getChannelCode()).isNull();
        assertThat(ContextHolder.getAccountCode()).isNull();
        assertThat(ContextHolder.getAccountType()).isNull();
        assertThat(ContextHolder.getAccountName()).isNull();
    }

    @Test
    void 设置后可读取() {
        ContextHolder.setChannelCode("CH001");
        ContextHolder.setAccountCode("ACC001");
        ContextHolder.setAccountType("admin");
        ContextHolder.setAccountName("张三");

        assertThat(ContextHolder.getChannelCode()).isEqualTo("CH001");
        assertThat(ContextHolder.getAccountCode()).isEqualTo("ACC001");
        assertThat(ContextHolder.getAccountType()).isEqualTo("admin");
        assertThat(ContextHolder.getAccountName()).isEqualTo("张三");
    }

    @Test
    void clear后所有字段为null() {
        ContextHolder.setChannelCode("CH001");
        ContextHolder.setAccountCode("ACC001");
        ContextHolder.clear();

        assertThat(ContextHolder.getChannelCode()).isNull();
        assertThat(ContextHolder.getAccountCode()).isNull();
    }

    @Test
    void 覆盖写入_取最新值() {
        ContextHolder.setChannelCode("CH001");
        ContextHolder.setChannelCode("CH002");
        assertThat(ContextHolder.getChannelCode()).isEqualTo("CH002");
    }

    @Test
    void systemOperator常量() {
        assertThat(ContextHolder.SYSTEM_OPERATOR).isEqualTo("system");
    }

    @Test
    void 线程隔离_各线程独立() throws InterruptedException {
        ContextHolder.setChannelCode("main-thread");

        Thread other = new Thread(() -> {
            // 子线程未设置，应为 null
            assertThat(ContextHolder.getChannelCode()).isNull();
            ContextHolder.setChannelCode("child-thread");
            assertThat(ContextHolder.getChannelCode()).isEqualTo("child-thread");
        });
        other.start();
        other.join();

        // 主线程不受子线程影响
        assertThat(ContextHolder.getChannelCode()).isEqualTo("main-thread");
    }
}
