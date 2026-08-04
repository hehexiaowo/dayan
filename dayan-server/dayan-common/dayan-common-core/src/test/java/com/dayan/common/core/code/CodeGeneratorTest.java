package com.dayan.common.core.code;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link CodeGenerator} 单元测试。
 */
class CodeGeneratorTest {

    @Test
    void generate_shouldReturnPrefixPlusPaddedNumber() {
        // 用 3 位宽度便于阅读
        CodeGenerator gen = new CodeGenerator(new InMemorySequenceProvider(), 3);
        String code = gen.generate("PK", 1L);
        assertThat(code).isEqualTo("PK001");
    }

    @Test
    void generate_shouldIncrementByDomainKey() {
        CodeGenerator gen = new CodeGenerator(new InMemorySequenceProvider(), 5);
        String a = gen.generate("PK", 1L);
        String b = gen.generate("PK", 1L);
        assertThat(a).isEqualTo("PK00001");
        assertThat(b).isEqualTo("PK00002");
    }

    @Test
    void generate_shouldIsolateDifferentPrefix() {
        CodeGenerator gen = new CodeGenerator(new InMemorySequenceProvider(), 5);
        String pk = gen.generate("PK", 1L);
        String sp = gen.generate("SP", 1L);
        assertThat(pk).isEqualTo("PK00001");
        assertThat(sp).isEqualTo("SP00001");
    }

    @Test
    void generate_shouldAppendChannelSuffixWhenProvided() {
        // 渠道隔离场景：同前缀不同 channel 各自计数
        CodeGenerator gen = new CodeGenerator(new InMemorySequenceProvider(), 5);
        String a1 = gen.generate("AG", 1001L);
        String a2 = gen.generate("AG", 1001L);
        String b1 = gen.generate("AG", 2002L);
        assertThat(a1).isEqualTo("AG00001");
        assertThat(a2).isEqualTo("AG00002");
        assertThat(b1).isEqualTo("AG00001");
    }

    @Test
    void generate_shouldBeUniqueUnderConcurrency() throws InterruptedException {
        CodeGenerator gen = new CodeGenerator(new InMemorySequenceProvider(), 5);
        int threads = 8;
        int perThread = 500;
        Set<String> codes = ConcurrentHashMap.newKeySet();
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(threads);

        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                try {
                    for (int j = 0; j < perThread; j++) {
                        codes.add(gen.generate("EQ", 1L));
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        latch.await();
        pool.shutdown();

        assertThat(codes).hasSize(threads * perThread);
    }

    /** 内存版 SequenceProvider，模拟 Redis INCR 的语义（线程安全 + 自增 + 按 key 隔离） */
    static class InMemorySequenceProvider implements SequenceProvider {
        private final java.util.concurrent.ConcurrentHashMap<String, AtomicLong> counter =
                new java.util.concurrent.ConcurrentHashMap<>();

        @Override
        public long next(String key) {
            return counter.computeIfAbsent(key, k -> new AtomicLong(0)).incrementAndGet();
        }
    }
}
