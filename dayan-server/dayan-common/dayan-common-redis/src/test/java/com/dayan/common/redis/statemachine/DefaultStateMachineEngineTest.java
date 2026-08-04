package com.dayan.common.redis.statemachine;

import com.dayan.common.core.exception.BusinessException;
import com.dayan.common.core.statemachine.StateRule;
import com.dayan.common.core.statemachine.StateRuleLoader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * {@link DefaultStateMachineEngine} 单元测试。
 */
class DefaultStateMachineEngineTest {

    private StringRedisTemplate redisTemplate;
    @SuppressWarnings("unchecked")
    private final HashOperations<String, Object, Object> hashOps = mock(HashOperations.class);
    @SuppressWarnings("unchecked")
    private final ObjectProvider<StateRuleLoader> loaderProvider = mock(ObjectProvider.class);
    private final StateRuleLoader loader = mock(StateRuleLoader.class);
    private DefaultStateMachineEngine engine;

    @BeforeEach
    void setUp() {
        redisTemplate = mock(StringRedisTemplate.class);
        when(redisTemplate.opsForHash()).thenReturn(hashOps);
        when(loaderProvider.getIfAvailable()).thenReturn(loader);
        engine = new DefaultStateMachineEngine(redisTemplate, loaderProvider);
    }

    @Test
    void checkTransition_shouldReturnToWhenRuleExists() {
        when(hashOps.get("dayan:sm:rule:EQUITY_SM", "0:activate")).thenReturn("2");
        int to = engine.checkTransition("EQUITY_SM", 0, "activate");
        assertThat(to).isEqualTo(2);
    }

    @Test
    void checkTransition_shouldThrowWhenRuleMissing() {
        when(hashOps.get(eq("dayan:sm:rule:EQUITY_SM"), any())).thenReturn(null);
        assertThatThrownBy(() -> engine.checkTransition("EQUITY_SM", 5, "activate"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("非法状态转移");
    }

    @Test
    void transition_shouldDelegateToCheck() {
        when(hashOps.get("dayan:sm:rule:ORDER_SM", "0:pay")).thenReturn("1");
        int to = engine.transition("ORDER_SM", 0, "pay");
        assertThat(to).isEqualTo(1);
    }

    @Test
    void loadRules_shouldPutAllIntoRedisHash() {
        when(loader.loadByDomain("EQUITY_SM")).thenReturn(List.of(
                new StateRule("EQUITY_SM", 0, "outbound", 1, ""),
                new StateRule("EQUITY_SM", 1, "activate", 2, ""),
                new StateRule("EQUITY_SM", 2, "use", 3, "")
        ));

        engine.loadRules("EQUITY_SM");

        verify(redisTemplate).delete("dayan:sm:rule:EQUITY_SM");
        verify(hashOps).putAll(eq("dayan:sm:rule:EQUITY_SM"), any(Map.class));
    }

    @Test
    void loadRules_shouldNoopWhenLoaderNull() {
        when(loaderProvider.getIfAvailable()).thenReturn(null);
        engine.loadRules("UNKNOWN_SM");
        verifyNoInteractions(loader);
    }

    @Test
    void loadAllRules_shouldGroupByDomainAndLoad() {
        when(loader.loadAll()).thenReturn(List.of(
                new StateRule("EQUITY_SM", 0, "outbound", 1, ""),
                new StateRule("ORDER_SM", 0, "pay", 1, "")
        ));

        engine.loadAllRules();

        verify(redisTemplate).delete("dayan:sm:rule:EQUITY_SM");
        verify(redisTemplate).delete("dayan:sm:rule:ORDER_SM");
        verify(hashOps, times(2)).putAll(anyString(), any(Map.class));
    }

    private static String anyString() {
        return org.mockito.ArgumentMatchers.anyString();
    }
}
