package com.dayan.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dayan.common.core.exception.BusinessException;
import com.dayan.common.core.exception.ErrorCode;
import com.dayan.common.core.resp.PageResult;
import com.dayan.common.mybatis.context.ContextHolder;
import com.dayan.system.entity.SystemConfig;
import com.dayan.system.entity.SystemConfigChangeRecord;
import com.dayan.system.mapper.SystemConfigChangeRecordMapper;
import com.dayan.system.mapper.SystemConfigMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

/**
 * 系统配置服务。
 *
 * <p>系统的外部平台核心凭据仓库（oss / map / sms / payment 等分组）：
 * 敏感值（is_secret=1）在分页/分组查询响应中统一脱敏为 {@link #SECRET_MASK}，
 * 更新时传入空值或掩码串即保留原值（编辑弹窗无需回显明文）。
 * 所有写操作落 {@link SystemConfigChangeRecord} 审计。
 */
@Service
@RequiredArgsConstructor
public class SystemConfigService {

    /** 敏感配置的响应掩码（更新传入此值或空串 = 保持原值） */
    public static final String SECRET_MASK = "******";

    private final SystemConfigMapper configMapper;
    private final SystemConfigChangeRecordMapper changeRecordMapper;

    /**
     * 分页查询（按 configGroup / configKey 过滤）。敏感值脱敏后返回。
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
        page.getRecords().forEach(SystemConfigService::maskSecret);
        return new PageResult<>(current, size, page.getTotal(), page.getRecords());
    }

    /**
     * 按 group 查全部配置项。敏感值脱敏后返回。
     */
    public List<SystemConfig> listByGroup(String configGroup) {
        List<SystemConfig> list = configMapper.selectList(new LambdaQueryWrapper<SystemConfig>()
                .eq(SystemConfig::getScope, "global")
                .eq(SystemConfig::getConfigGroup, configGroup)
                .orderByAsc(SystemConfig::getSortOrder));
        list.forEach(SystemConfigService::maskSecret);
        return list;
    }

    /**
     * 按 key 查配置值（明文，仅服务端内部消费，如 OSS 凭据供应、地图 key 下发）。
     */
    public String getValue(String configKey) {
        SystemConfig config = configMapper.selectOne(new LambdaQueryWrapper<SystemConfig>()
                .eq(SystemConfig::getConfigKey, configKey)
                .eq(SystemConfig::getScope, "global")
                .last("LIMIT 1"));
        return config == null ? null : config.getConfigValue();
    }

    /**
     * 按分组 + key 查配置值（明文）。configKey 全仓约定带分组前缀（如 oss.endpoint），
     * 本重载供明确知道分组的调用方使用，避免跨组同名误读。
     */
    public String getValue(String configGroup, String configKey) {
        SystemConfig config = configMapper.selectOne(new LambdaQueryWrapper<SystemConfig>()
                .eq(SystemConfig::getConfigGroup, configGroup)
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
        recordChange(config, "create", null, config.getConfigValue());
        return config.getConfigKey();
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(String configKey, SystemConfig config) {
        SystemConfig existing = configMapper.selectOne(new LambdaQueryWrapper<SystemConfig>()
                .eq(SystemConfig::getConfigKey, configKey).last("LIMIT 1"));
        if (existing == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "配置不存在: " + configKey);
        }
        // 敏感配置：传入空值或掩码串 = 保持原值（列表已脱敏，编辑无需回显明文）
        boolean keepSecret = Objects.equals(existing.getIsSecret(), 1)
                && (config.getConfigValue() == null || config.getConfigValue().isBlank()
                        || SECRET_MASK.equals(config.getConfigValue()));
        if (keepSecret) {
            config.setConfigValue(existing.getConfigValue());
        }
        config.setId(existing.getId());
        config.setConfigKey(existing.getConfigKey());
        configMapper.updateById(config);
        recordChange(existing, "update", existing.getConfigValue(),
                keepSecret ? existing.getConfigValue() : config.getConfigValue());
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(String configKey) {
        SystemConfig existing = configMapper.selectOne(new LambdaQueryWrapper<SystemConfig>()
                .eq(SystemConfig::getConfigKey, configKey).last("LIMIT 1"));
        if (existing == null) {
            return;
        }
        configMapper.delete(new LambdaQueryWrapper<SystemConfig>()
                .eq(SystemConfig::getConfigKey, configKey));
        recordChange(existing, "delete", existing.getConfigValue(), null);
    }

    /** 响应脱敏：敏感配置的值统一替换为掩码串（空值敏感项不掩码，便于识别未配置） */
    private static void maskSecret(SystemConfig config) {
        if (Objects.equals(config.getIsSecret(), 1)
                && config.getConfigValue() != null && !config.getConfigValue().isBlank()) {
            config.setConfigValue(SECRET_MASK);
        }
    }

    /** 写变更审计记录（敏感值掩码落库，防审计表成为第二泄露面） */
    private void recordChange(SystemConfig config, String action, String oldValue, String newValue) {
        try {
            SystemConfigChangeRecord record = new SystemConfigChangeRecord();
            record.setConfigId(config.getId());
            record.setConfigGroup(config.getConfigGroup());
            record.setConfigKey(config.getConfigKey());
            record.setEnv(config.getEnv());
            record.setOldValue(Objects.equals(config.getIsSecret(), 1) ? SECRET_MASK : oldValue);
            record.setNewValue(Objects.equals(config.getIsSecret(), 1) ? SECRET_MASK : newValue);
            record.setAction(action);
            record.setAccountType(ContextHolder.getAccountType());
            record.setAccountCode(ContextHolder.getAccountCode());
            changeRecordMapper.insert(record);
        } catch (Exception e) {
            // 审计失败不阻断配置写入
            org.slf4j.LoggerFactory.getLogger(SystemConfigService.class)
                    .warn("系统配置变更记录写入失败 key={}: {}", config.getConfigKey(), e.getMessage());
        }
    }
}
