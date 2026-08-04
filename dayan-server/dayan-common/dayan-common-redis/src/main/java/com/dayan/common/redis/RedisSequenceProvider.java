package com.dayan.common.redis;

import com.dayan.common.core.code.SequenceProvider;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.concurrent.TimeUnit;

/**
 * 基于 Redis 的序列号提供者。
 *
 * <p>使用 {@code INCR} 命令实现跨实例、并发安全的自增序列，
 * 供 {@link com.dayan.common.core.code.CodeGenerator} 生成业务编码（如 PK00001）。
 */
public class RedisSequenceProvider implements SequenceProvider {

    private final StringRedisTemplate redisTemplate;

    public RedisSequenceProvider(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public long next(String key) {
        Long value = redisTemplate.opsForValue().increment(key);
        if (value == null) {
            throw new IllegalStateException("Redis INCR 返回 null: " + key);
        }
        // 首次创建时设置过期时间，避免无界增长（30 天后自动清理空闲序列）
        if (value == 1L) {
            redisTemplate.expire(key, 30, TimeUnit.DAYS);
        }
        return value;
    }
}
