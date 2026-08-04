package com.dayan.common.core.snowflake;

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link SnowflakeId} 单元测试。
 */
class SnowflakeIdTest {

    @Test
    void nextId_shouldBePositive() {
        SnowflakeId snowflake = new SnowflakeId(1L, 1L);
        long id = snowflake.nextId();
        assertThat(id).isPositive();
    }

    @RepeatedTest(100)
    void nextId_shouldBeMonotonicIncreasingInSameProcess() {
        SnowflakeId snowflake = new SnowflakeId(1L, 1L);
        long a = snowflake.nextId();
        long b = snowflake.nextId();
        assertThat(b).isGreaterThan(a);
    }

    @Test
    void nextId_shouldBeUniqueUnderConcurrency() throws InterruptedException {
        SnowflakeId snowflake = new SnowflakeId(1L, 1L);
        int threads = 16;
        int perThread = 1000;
        Set<Long> ids = ConcurrentHashMap.newKeySet();
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(threads);

        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                try {
                    for (int j = 0; j < perThread; j++) {
                        ids.add(snowflake.nextId());
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        latch.await();
        pool.shutdown();

        int expected = threads * perThread;
        assertThat(ids).hasSize(expected);
    }

    @Test
    void nextId_shouldEncodeWorkerAndDatacenter() {
        long datacenterId = 5L;
        long workerId = 7L;
        SnowflakeId snowflake = new SnowflakeId(datacenterId, workerId);
        long id = snowflake.nextId();
        // timestamp 占高位，datacenter(5bit)+worker(5bit) 在时间戳之后
        // 解析：workerIdBits=5、datacenterIdBits=5，sequenceBits=12
        long workerPart = (id >> 12) & 0x1F;
        long datacenterPart = (id >> 17) & 0x1F;
        assertThat(workerPart).isEqualTo(workerId);
        assertThat(datacenterPart).isEqualTo(datacenterId);
    }
}
