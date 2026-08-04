package com.dayan.common.redis.statemachine;

import com.dayan.common.core.exception.BusinessException;
import com.dayan.common.core.exception.ErrorCode;
import com.dayan.common.core.statemachine.StateRule;
import com.dayan.common.core.statemachine.StateMachineEngine;
import com.dayan.common.core.statemachine.StateRuleLoader;
import com.dayan.common.redis.RedisKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 状态机引擎默认实现（Redis Hash 缓存规则）。
 *
 * <p>缓存结构：Redis Hash {@code dayan:sm:rule:{domain}}，
 * field = {@code {from}:{event}}，value = {@code to}（字符串）。
 *
 * <p>规则数据源 {@link StateRuleLoader} 由 system 业务模块提供（可选，无则缓存为空，
 * 转移全部抛异常——用于尚未初始化规则的域）。
 */
@Component
public class DefaultStateMachineEngine implements StateMachineEngine {

    private static final Logger log = LoggerFactory.getLogger(DefaultStateMachineEngine.class);

    private final StringRedisTemplate redisTemplate;
    private final ObjectProvider<StateRuleLoader> ruleLoaderProvider;

    public DefaultStateMachineEngine(StringRedisTemplate redisTemplate,
                                     ObjectProvider<StateRuleLoader> ruleLoaderProvider) {
        this.redisTemplate = redisTemplate;
        this.ruleLoaderProvider = ruleLoaderProvider;
    }

    @Override
    public int checkTransition(String domain, int from, String event) {
        String key = RedisKey.smRule(domain);
        String field = from + ":" + event;
        Object to = redisTemplate.opsForHash().get(key, field);
        if (to == null) {
            throw new BusinessException(ErrorCode.BUSINESS,
                    "非法状态转移: domain=" + domain + ", from=" + from + ", event=" + event);
        }
        return Integer.parseInt(to.toString());
    }

    @Override
    public int transition(String domain, int from, String event) {
        int to = checkTransition(domain, from, event);
        log.debug("状态转移: domain={}, {} --{}--> {}", domain, from, event, to);
        return to;
    }

    @Override
    public void loadRules(String domain) {
        StateRuleLoader loader = ruleLoaderProvider.getIfAvailable();
        if (loader == null) {
            log.warn("StateRuleLoader 未实现，状态机域 {} 规则未加载", domain);
            return;
        }
        List<StateRule> rules = loader.loadByDomain(domain);
        if (rules == null || rules.isEmpty()) {
            log.warn("状态机域 {} 无规则数据", domain);
            return;
        }
        Map<String, String> ruleMap = new HashMap<>(rules.size());
        for (StateRule rule : rules) {
            ruleMap.put(rule.getFromStatus() + ":" + rule.getEvent(),
                    String.valueOf(rule.getToStatus()));
        }
        String key = RedisKey.smRule(domain);
        redisTemplate.delete(key);
        redisTemplate.opsForHash().putAll(key, ruleMap);
        log.info("状态机域 {} 加载 {} 条规则", domain, rules.size());
    }

    @Override
    public void refreshRules(String domain) {
        loadRules(domain);
    }

    /**
     * 应用启动后预热全部域规则（由 system 模块在就绪后触发）。
     */
    public void loadAllRules() {
        StateRuleLoader loader = ruleLoaderProvider.getIfAvailable();
        if (loader == null) {
            return;
        }
        List<StateRule> all = loader.loadAll();
        if (all == null) {
            return;
        }
        Map<String, Map<String, String>> byDomain = new HashMap<>();
        for (StateRule rule : all) {
            byDomain.computeIfAbsent(rule.getDomain(), k -> new HashMap<>())
                    .put(rule.getFromStatus() + ":" + rule.getEvent(),
                            String.valueOf(rule.getToStatus()));
        }
        for (Map.Entry<String, Map<String, String>> e : byDomain.entrySet()) {
            String key = RedisKey.smRule(e.getKey());
            redisTemplate.delete(key);
            redisTemplate.opsForHash().putAll(key, e.getValue());
            log.info("状态机域 {} 预热 {} 条规则", e.getKey(), e.getValue().size());
        }
    }
}
