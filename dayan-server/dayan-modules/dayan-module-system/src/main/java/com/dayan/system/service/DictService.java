package com.dayan.system.service;

import com.dayan.system.entity.SystemDict;
import com.dayan.common.core.exception.BusinessException;
import com.dayan.common.core.exception.ErrorCode;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    private final com.dayan.system.mapper.SystemDictMapper dictMapper;
    private final ObjectMapper objectMapper;

    /**
     * 按类型查询字典项列表（命中缓存）。
     */
    public List<SystemDict> getByType(String dictType) {
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
        List<SystemDict> list = dictMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SystemDict>()
                        .eq(SystemDict::getDictType, dictType)
                        .eq(SystemDict::getStatus, 1)
                        .orderByAsc(SystemDict::getSortOrder));
        cacheByType(dictType, list);
        return list;
    }

    /**
     * 按类型 + code 查单个字典项。
     */
    public SystemDict getByCode(String dictType, String dictCode) {
        return getByType(dictType).stream()
                .filter(d -> dictCode.equals(d.getDictCode()))
                .findFirst()
                .orElse(null);
    }

    /**
     * 缓存指定类型（供写入后调用刷新）。
     */
    public void cacheByType(String dictType, List<SystemDict> list) {
        String key = CACHE_PREFIX + dictType;
        try {
            Map<String, String> hash = new HashMap<>();
            for (SystemDict d : list) {
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

    /**
     * 全部字典类型枚举（distinct dict_type，供前端左侧类型选择）。
     */
    public List<String> listTypes() {
        List<SystemDict> all = dictMapper.selectList(null);
        return all.stream()
                .map(SystemDict::getDictType)
                .filter(t -> t != null && !t.isEmpty())
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    /**
     * 按类型查询全部字典项（含禁用，管理页用；不过滤 status、不走缓存）。
     */
    public List<SystemDict> listAllByType(String dictType) {
        return dictMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SystemDict>()
                        .eq(SystemDict::getDictType, dictType)
                        .orderByAsc(SystemDict::getSortOrder));
    }

    /**
     * 新增字典项。（dictType, dictCode）唯一校验，写入后失效缓存。
     */
    @Transactional(rollbackFor = Exception.class)
    public Long create(SystemDict dict) {
        Long count = dictMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SystemDict>()
                        .eq(SystemDict::getDictType, dict.getDictType())
                        .eq(SystemDict::getDictCode, dict.getDictCode()));
        if (count > 0) {
            throw new BusinessException(ErrorCode.BUSINESS, "字典编码已存在: " + dict.getDictCode());
        }
        if (dict.getStatus() == null) dict.setStatus(1);
        if (dict.getIsDefault() == null) dict.setIsDefault(0);
        if (dict.getSortOrder() == null) dict.setSortOrder(0);
        dictMapper.insert(dict);
        evict(dict.getDictType());
        return dict.getId();
    }

    /**
     * 修改字典项（按 id）。新旧 dictType 缓存均失效。
     */
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, SystemDict dict) {
        SystemDict existing = dictMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "字典项不存在: id=" + id);
        }
        dict.setId(id);
        // dictCode 变更时校验新 (dictType, dictCode) 唯一
        String newCode = dict.getDictCode();
        if (newCode != null && !newCode.equals(existing.getDictCode())) {
            Long count = dictMapper.selectCount(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SystemDict>()
                            .eq(SystemDict::getDictType, existing.getDictType())
                            .eq(SystemDict::getDictCode, newCode));
            if (count > 0) {
                throw new BusinessException(ErrorCode.BUSINESS, "字典编码已存在: " + newCode);
            }
        }
        dictMapper.updateById(dict);
        evict(existing.getDictType());
        if (dict.getDictType() != null && !dict.getDictType().equals(existing.getDictType())) {
            evict(dict.getDictType());
        }
    }

    /**
     * 删除字典项（按 id）。失效对应类型缓存。
     */
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        SystemDict existing = dictMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "字典项不存在: id=" + id);
        }
        dictMapper.deleteById(id);
        evict(existing.getDictType());
    }

    private String serialize(SystemDict d) {
        try {
            return objectMapper.writeValueAsString(d);
        } catch (Exception e) {
            return "{}";
        }
    }

    private SystemDict deserialize(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            return new SystemDict();
        }
    }
}
