package com.dayan.common.redis;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * {@link RedisSequenceProvider} 单元测试（Mock StringRedisTemplate）。
 */
class RedisSequenceProviderTest {

    @Test
    void next_shouldIncrementAndReturn() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> ops = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(ops);
        when(ops.increment("dayan:code:seq:PK:0")).thenReturn(5L);

        RedisSequenceProvider provider = new RedisSequenceProvider(redis);
        long val = provider.next("dayan:code:seq:PK:0");
        assertThat(val).isEqualTo(5L);
    }

    @Test
    void next_shouldSetExpiryOnFirstIncrement() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> ops = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(ops);
        when(ops.increment(anyString())).thenReturn(1L);

        RedisSequenceProvider provider = new RedisSequenceProvider(redis);
        provider.next("dayan:code:seq:AG:1001");

        verify(redis).expire(eq("dayan:code:seq:AG:1001"), eq(30L), eq(TimeUnit.DAYS));
    }

    @Test
    void next_shouldNotSetExpiryOnSubsequentIncrement() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> ops = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(ops);
        when(ops.increment(anyString())).thenReturn(2L);

        RedisSequenceProvider provider = new RedisSequenceProvider(redis);
        provider.next("dayan:code:seq:AG:1001");

        verify(redis, never()).expire(anyString(), anyLong(), any(TimeUnit.class));
    }
}
