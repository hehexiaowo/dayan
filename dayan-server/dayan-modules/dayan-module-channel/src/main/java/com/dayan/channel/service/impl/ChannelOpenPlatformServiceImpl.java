package com.dayan.channel.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dayan.channel.dto.ChannelOpenPlatformCreateDTO;
import com.dayan.channel.dto.ChannelOpenPlatformQueryDTO;
import com.dayan.channel.dto.ChannelOpenPlatformUpdateDTO;
import com.dayan.channel.entity.ChannelOpenPlatform;
import com.dayan.channel.mapper.ChannelOpenPlatformMapper;
import com.dayan.channel.service.ChannelOpenPlatformService;
import com.dayan.channel.vo.ChannelOpenPlatformVO;
import com.dayan.common.core.crypto.AesGcmUtil;
import com.dayan.common.core.exception.BusinessException;
import com.dayan.common.core.exception.ErrorCode;
import com.dayan.common.core.resp.PageResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 渠道开放平台配置服务实现。
 *
 * <p>密钥管理：
 * <ul>
 *   <li>加密密钥从配置 {@code dayan.aes.key} 读取；为空时回退到
 *       {@link AesGcmUtil#deriveKey(String)}("dayan-default-key") 派生（仅用于开发/测试环境）</li>
 *   <li>create/update 时对明文 {@code appSecret} 执行 {@link AesGcmUtil#encrypt}</li>
 *   <li>查询时脱敏为 {@code ***}，明文不回传</li>
 * </ul>
 *
 * <p>注意：因 {@code aesKeyHex} 需由 {@code @Value} 在构造时派生，未使用
 * {@code @RequiredArgsConstructor}，改为显式构造器。
 */
@Slf4j
@Service
public class ChannelOpenPlatformServiceImpl implements ChannelOpenPlatformService {

    /** appSecret 出参脱敏占位 */
    private static final String SECRET_MASK = "***";
    /** 默认 AES 派生密钥（仅开发/测试回退用） */
    private static final String DEFAULT_KEY_PASSWORD = "dayan-default-key";

    private final ChannelOpenPlatformMapper openPlatformMapper;

    /** AES 密钥 hex（由配置 dayan.aes.key 派生） */
    private final String aesKeyHex;

    public ChannelOpenPlatformServiceImpl(
            ChannelOpenPlatformMapper openPlatformMapper,
            @Value("${dayan.aes.key:}") String configuredKey) {
        this.openPlatformMapper = openPlatformMapper;
        if (configuredKey == null || configuredKey.isBlank()) {
            this.aesKeyHex = AesGcmUtil.deriveKey(DEFAULT_KEY_PASSWORD);
            log.warn("未配置 dayan.aes.key，回退使用默认派生密钥（仅供开发/测试）");
        } else {
            this.aesKeyHex = AesGcmUtil.deriveKey(configuredKey);
        }
    }

    @Override
    public PageResult<ChannelOpenPlatformVO> page(ChannelOpenPlatformQueryDTO query) {
        LambdaQueryWrapper<ChannelOpenPlatform> wrapper = new LambdaQueryWrapper<ChannelOpenPlatform>()
                .orderByDesc(ChannelOpenPlatform::getCreatedAt);
        if (query.getChannelCode() != null && !query.getChannelCode().isEmpty()) {
            wrapper.eq(ChannelOpenPlatform::getChannelCode, query.getChannelCode());
        }
        if (query.getPlatformName() != null && !query.getPlatformName().isEmpty()) {
            wrapper.like(ChannelOpenPlatform::getPlatformName, query.getPlatformName());
        }
        if (query.getDockType() != null) {
            wrapper.eq(ChannelOpenPlatform::getDockType, query.getDockType());
        }
        if (query.getStatus() != null) {
            wrapper.eq(ChannelOpenPlatform::getStatus, query.getStatus());
        }
        Page<ChannelOpenPlatform> page = openPlatformMapper.selectPage(
                new Page<>(query.getCurrent(), query.getSize()), wrapper);
        List<ChannelOpenPlatformVO> records = page.getRecords().stream().map(this::toVO).toList();
        return new PageResult<>(query.getCurrent(), query.getSize(), page.getTotal(), records);
    }

    @Override
    public ChannelOpenPlatformVO getDetail(Long id) {
        return toVO(requireById(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(ChannelOpenPlatformCreateDTO dto) {
        ChannelOpenPlatform entity = new ChannelOpenPlatform();
        entity.setChannelCode(dto.getChannelCode());
        entity.setPlatformName(dto.getPlatformName());
        entity.setDockType(dto.getDockType());
        entity.setApiBaseUrl(dto.getApiBaseUrl());
        entity.setAppKey(dto.getAppKey());
        entity.setAppSecret(encryptSecret(dto.getAppSecret()));
        entity.setCallbackUrl(dto.getCallbackUrl());
        entity.setH5Domain(dto.getH5Domain());
        entity.setH5Theme(dto.getH5Theme());
        entity.setAuthType(dto.getAuthType());
        entity.setIpWhitelist(dto.getIpWhitelist());
        entity.setRateLimit(dto.getRateLimit());
        entity.setTimeout(dto.getTimeout());
        entity.setExtraConfig(dto.getExtraConfig());
        entity.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        openPlatformMapper.insert(entity);
        log.info("创建开放平台配置成功: id={}, channelCode={}, platformName={}",
                entity.getId(), entity.getChannelCode(), entity.getPlatformName());
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, ChannelOpenPlatformUpdateDTO dto) {
        ChannelOpenPlatform existing = requireById(id);
        ChannelOpenPlatform update = new ChannelOpenPlatform();
        update.setId(existing.getId());
        if (dto.getPlatformName() != null) update.setPlatformName(dto.getPlatformName());
        if (dto.getDockType() != null) update.setDockType(dto.getDockType());
        if (dto.getApiBaseUrl() != null) update.setApiBaseUrl(dto.getApiBaseUrl());
        if (dto.getAppKey() != null) update.setAppKey(dto.getAppKey());
        // appSecret 非空才轮换（避免误清空）
        if (dto.getAppSecret() != null && !dto.getAppSecret().isEmpty()) {
            update.setAppSecret(encryptSecret(dto.getAppSecret()));
        }
        if (dto.getCallbackUrl() != null) update.setCallbackUrl(dto.getCallbackUrl());
        if (dto.getH5Domain() != null) update.setH5Domain(dto.getH5Domain());
        if (dto.getH5Theme() != null) update.setH5Theme(dto.getH5Theme());
        if (dto.getAuthType() != null) update.setAuthType(dto.getAuthType());
        if (dto.getIpWhitelist() != null) update.setIpWhitelist(dto.getIpWhitelist());
        if (dto.getRateLimit() != null) update.setRateLimit(dto.getRateLimit());
        if (dto.getTimeout() != null) update.setTimeout(dto.getTimeout());
        if (dto.getExtraConfig() != null) update.setExtraConfig(dto.getExtraConfig());
        if (dto.getStatus() != null) update.setStatus(dto.getStatus());
        openPlatformMapper.updateById(update);
        log.info("更新开放平台配置成功: id={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        requireById(id);
        openPlatformMapper.deleteById(id);
        log.info("删除开放平台配置成功: id={}", id);
    }

    // ====== 内部方法 ======

    private ChannelOpenPlatform requireById(Long id) {
        ChannelOpenPlatform entity = openPlatformMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "开放平台配置不存在: id=" + id);
        }
        return entity;
    }

    private String encryptSecret(String plaintext) {
        if (plaintext == null || plaintext.isEmpty()) {
            return null;
        }
        return AesGcmUtil.encrypt(plaintext, aesKeyHex);
    }

    private ChannelOpenPlatformVO toVO(ChannelOpenPlatform entity) {
        ChannelOpenPlatformVO vo = new ChannelOpenPlatformVO();
        vo.setId(entity.getId());
        vo.setChannelCode(entity.getChannelCode());
        vo.setPlatformName(entity.getPlatformName());
        vo.setDockType(entity.getDockType());
        vo.setApiBaseUrl(entity.getApiBaseUrl());
        vo.setAppKey(entity.getAppKey());
        // 密钥脱敏：明文不回传
        vo.setAppSecret(entity.getAppSecret() == null ? null : SECRET_MASK);
        vo.setCallbackUrl(entity.getCallbackUrl());
        vo.setH5Domain(entity.getH5Domain());
        vo.setH5Theme(entity.getH5Theme());
        vo.setAuthType(entity.getAuthType());
        vo.setIpWhitelist(entity.getIpWhitelist());
        vo.setRateLimit(entity.getRateLimit());
        vo.setTimeout(entity.getTimeout());
        vo.setExtraConfig(entity.getExtraConfig());
        vo.setStatus(entity.getStatus());
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }
}
