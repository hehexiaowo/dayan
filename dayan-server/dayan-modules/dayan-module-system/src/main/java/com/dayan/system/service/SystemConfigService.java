package com.dayan.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dayan.common.core.exception.BusinessException;
import com.dayan.common.core.exception.ErrorCode;
import com.dayan.common.core.resp.PageResult;
import com.dayan.system.entity.SystemConfig;
import com.dayan.system.mapper.SystemConfigMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 系统配置服务。
 *
 * <p>P1 仅实现 global 级配置 CRUD（scope=global）。
 * 多级配置（organ/user）+ 热更新在 P3 阶段补。
 */
@Service
@RequiredArgsConstructor
public class SystemConfigService {

    private final SystemConfigMapper configMapper;

    /**
     * 分页查询（按 configGroup / configKey 过滤）。
     */
    public PageResult<SystemConfig> page(long current, long size, String configGroup, String configKey) {
        LambdaQueryWrapper<SystemConfig> wrapper = new LambdaQueryWrapper<SystemConfig>()
                .eq(SystemConfig::getScope, "global")
                .orderByAsc(SystemConfig::getSortOrder);
        if (configGroup != null && !configGroup.isEmpty()) {
            wrapper.eq(SystemConfig::getConfigGroup, configGroup);
        }
        if (configKey != null && !configKey.isEmpty()) {
            wrapper.like(SystemConfig::getConfigKey, configKey);
        }
        Page<SystemConfig> page = configMapper.selectPage(new Page<>(current, size), wrapper);
        return new PageResult<>(current, size, page.getTotal(), page.getRecords());
    }

    /**
     * 按 group 查全部配置项。
     */
    public List<SystemConfig> listByGroup(String configGroup) {
        return configMapper.selectList(new LambdaQueryWrapper<SystemConfig>()
                .eq(SystemConfig::getScope, "global")
                .eq(SystemConfig::getConfigGroup, configGroup)
                .orderByAsc(SystemConfig::getSortOrder));
    }

    /**
     * 按 key 查配置值。
     */
    public String getValue(String configKey) {
        SystemConfig config = configMapper.selectOne(new LambdaQueryWrapper<SystemConfig>()
                .eq(SystemConfig::getConfigKey, configKey)
                .eq(SystemConfig::getScope, "global")
                .last("LIMIT 1"));
        return config == null ? null : config.getConfigValue();
    }

    @Transactional(rollbackFor = Exception.class)
    public String create(SystemConfig config) {
        // configKey 全局唯一校验
        Long count = configMapper.selectCount(new LambdaQueryWrapper<SystemConfig>()
                .eq(SystemConfig::getConfigKey, config.getConfigKey()));
        if (count > 0) {
            throw new BusinessException(ErrorCode.BUSINESS, "配置键已存在: " + config.getConfigKey());
        }
        config.setScope("global");
        configMapper.insert(config);
        return config.getConfigKey();
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(String configKey, SystemConfig config) {
        SystemConfig existing = configMapper.selectOne(new LambdaQueryWrapper<SystemConfig>()
                .eq(SystemConfig::getConfigKey, configKey).last("LIMIT 1"));
        if (existing == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "配置不存在: " + configKey);
        }
        config.setId(existing.getId());
        configMapper.updateById(config);
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(String configKey) {
        configMapper.delete(new LambdaQueryWrapper<SystemConfig>()
                .eq(SystemConfig::getConfigKey, configKey));
    }
}
