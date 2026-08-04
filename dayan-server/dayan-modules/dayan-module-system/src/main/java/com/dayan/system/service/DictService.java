package com.dayan.system.service;

import com.dayan.system.entity.SystemDictCommon;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 字典缓存服务。
 *
 * <p>按 dictType 缓存到 Redis Hash {@code dayan:dict:{type}}，field=dict_code，value=字典项 JSON。
 * 查询优先命中缓存，未命中回源 DB 并回填；增删改后删除对应 type 缓存。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DictService {

    private static final String CACHE_PREFIX = "dayan:dict:";
    private static final long CACHE_TTL_HOURS = 24;

    private final StringRedisTemplate redisTemplate;
    private final com.dayan.system.mapper.SystemDictCommonMapper dictMapper;
    private final ObjectMapper objectMapper;

    /**
     * 按类型查询字典项列表（命中缓存）。
     */
    public List<SystemDictCommon> getByType(String dictType) {
        String key = CACHE_PREFIX + dictType;
        try {
            List<Object> values = redisTemplate.opsForHash().values(key);
            if (!values.isEmpty()) {
                return values.stream()
                        .map(v -> deserialize(v.toString()))
                        .sorted(Comparator.comparingInt(d -> d.getSortOrder() == null ? 0 : d.getSortOrder()))
                        .collect(Collectors.toList());
            }
        } catch (Exception e) {
            log.warn("字典缓存读取失败 type={}, 回源DB: {}", dictType, e.getMessage());
        }
        // 回源 DB
        List<SystemDictCommon> list = dictMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SystemDictCommon>()
                        .eq(SystemDictCommon::getDictType, dictType)
                        .eq(SystemDictCommon::getStatus, 1)
                        .orderByAsc(SystemDictCommon::getSortOrder));
        cacheByType(dictType, list);
        return list;
    }

    /**
     * 按类型 + code 查单个字典项。
     */
    public SystemDictCommon getByCode(String dictType, String dictCode) {
        return getByType(dictType).stream()
                .filter(d -> dictCode.equals(d.getDictCode()))
                .findFirst()
                .orElse(null);
    }

    /**
     * 缓存指定类型（供写入后调用刷新）。
     */
    public void cacheByType(String dictType, List<SystemDictCommon> list) {
        String key = CACHE_PREFIX + dictType;
        try {
            java.util.Map<String, String> hash = new java.util.HashMap<>();
            for (SystemDictCommon d : list) {
                hash.put(d.getDictCode(), serialize(d));
            }
            redisTemplate.opsForHash().putAll(key, hash);
            redisTemplate.expire(key, CACHE_TTL_HOURS, TimeUnit.HOURS);
        } catch (Exception e) {
            log.warn("字典缓存写入失败 type={}: {}", dictType, e.getMessage());
        }
    }

    /**
     * 失效指定类型缓存（增删改后调用）。
     */
    public void evict(String dictType) {
        redisTemplate.delete(CACHE_PREFIX + dictType);
    }

    private String serialize(SystemDictCommon d) {
        try {
            return objectMapper.writeValueAsString(d);
        } catch (Exception e) {
            return "{}";
        }
    }

    private SystemDictCommon deserialize(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            return new SystemDictCommon();
        }
    }
}
