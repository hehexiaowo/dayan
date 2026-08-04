package com.dayan.common.core.snowflake;

/**
 * 雪花算法 ID 生成器。
 *
 * <p>结构（共 64 bit，首位为符号位 0）：
 * <pre>
 *  41 bit 时间戳（毫秒，相对起始纪元） | 5 bit 数据中心 | 5 bit 机器 | 12 bit 序列号
 * </pre>
 *
 * <p>单机每毫秒可生成 4096 个 ID，41 bit 时间戳可用约 69 年。
 * 线程安全：通过 synchronized 保证序列号分配原子性。
 */
public class SnowflakeId {

    /** 起始纪元：2026-01-01 00:00:00 UTC（毫秒） */
    private static final long EPOCH = 1767225600000L;

    /** 机器 ID 占用位数 */
    private static final long WORKER_ID_BITS = 5L;
    /** 数据中心 ID 占用位数 */
    private static final long DATACENTER_ID_BITS = 5L;
    /** 序列号占用位数 */
    private static final long SEQUENCE_BITS = 12L;

    /** 最大机器 ID：31 */
    private static final long MAX_WORKER_ID = ~(-1L << WORKER_ID_BITS);
    /** 最大数据中心 ID：31 */
    private static final long MAX_DATACENTER_ID = ~(-1L << DATACENTER_ID_BITS);
    /** 序列号掩码：4095 */
    private static final long SEQUENCE_MASK = ~(-1L << SEQUENCE_BITS);

    /** 机器 ID 左移位数：12 */
    private static final long WORKER_ID_SHIFT = SEQUENCE_BITS;
    /** 数据中心 ID 左移位数：17 */
    private static final long DATACENTER_ID_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS;
    /** 时间戳左移位数：22 */
    private static final long TIMESTAMP_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS + DATACENTER_ID_BITS;

    private final long workerId;
    private final long datacenterId;

    /** 当前毫秒内序列号 */
    private long sequence = 0L;
    /** 上次生成 ID 的时间戳 */
    private long lastTimestamp = -1L;

    /**
     * @param datacenterId 数据中心 ID（0-31）
     * @param workerId     机器 ID（0-31）
     */
    public SnowflakeId(long datacenterId, long workerId) {
        if (workerId > MAX_WORKER_ID || workerId < 0) {
            throw new IllegalArgumentException(
                    "workerId 非法: " + workerId + " (应在 0-" + MAX_WORKER_ID + " 之间)");
        }
        if (datacenterId > MAX_DATACENTER_ID || datacenterId < 0) {
            throw new IllegalArgumentException(
                    "datacenterId 非法: " + datacenterId + " (应在 0-" + MAX_DATACENTER_ID + " 之间)");
        }
        this.workerId = workerId;
        this.datacenterId = datacenterId;
    }

    /**
     * 生成下一个 ID。线程安全。
     */
    public synchronized long nextId() {
        long timestamp = System.currentTimeMillis();

        if (timestamp < lastTimestamp) {
            // 时钟回拨：等待到上次时间
            timestamp = tilNextMillis(lastTimestamp);
        }

        if (timestamp == lastTimestamp) {
            sequence = (sequence + 1) & SEQUENCE_MASK;
            if (sequence == 0L) {
                // 当前毫秒序列耗尽，等待下一毫秒
                timestamp = tilNextMillis(lastTimestamp);
            }
        } else {
            sequence = 0L;
        }

        lastTimestamp = timestamp;

        return ((timestamp - EPOCH) << TIMESTAMP_SHIFT)
                | (datacenterId << DATACENTER_ID_SHIFT)
                | (workerId << WORKER_ID_SHIFT)
                | sequence;
    }

    private long tilNextMillis(long lastTimestamp) {
        long timestamp = System.currentTimeMillis();
        while (timestamp <= lastTimestamp) {
            timestamp = System.currentTimeMillis();
        }
        return timestamp;
    }
}
