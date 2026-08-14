package com.dayan.supplier.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dayan.common.core.crypto.AesGcmUtil;
import com.dayan.common.core.exception.BusinessException;
import com.dayan.common.core.exception.ErrorCode;
import com.dayan.common.core.resp.PageResult;
import com.dayan.supplier.dto.SupplierOpenPlatformCreateDTO;
import com.dayan.supplier.dto.SupplierOpenPlatformQueryDTO;
import com.dayan.supplier.dto.SupplierOpenPlatformUpdateDTO;
import com.dayan.supplier.entity.SupplierOpenPlatform;
import com.dayan.supplier.mapper.SupplierOpenPlatformMapper;
import com.dayan.supplier.service.SupplierOpenPlatformService;
import com.dayan.supplier.vo.SupplierOpenPlatformVO;
import com.dayan.common.security.secret.DayanSecrets;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 供应商开放平台配置服务实现。
 *
 * <p>密钥管理：
 * <ul>
 *   <li>加密密钥由 {@link com.dayan.common.security.secret.DayanSecrets#aesKeyHex()} 单点提供（配置项 {@code dayan.aes.key}，生产必须显式配置）</li>
 *   <li>create/update 时对明文 {@code appSecret} / {@code webhookSecret} 执行 {@link AesGcmUtil#encrypt}</li>
 *   <li>查询时脱敏为 {@code ***}，明文不回传</li>
 * </ul>
 *
 * <p>注意：AES 密钥由 {@link com.dayan.common.security.secret.DayanSecrets} 单点提供，
 * 需显式构造器注入，未使用 {@code @RequiredArgsConstructor}。
 */
@Slf4j
@Service
public class SupplierOpenPlatformServiceImpl implements SupplierOpenPlatformService {

    /** 密钥出参脱敏占位 */
    private static final String SECRET_MASK = "***";
    private final SupplierOpenPlatformMapper openPlatformMapper;

    /** AES 密钥 hex（由配置 dayan.aes.key 派生） */
    private final String aesKeyHex;

    public SupplierOpenPlatformServiceImpl(
            SupplierOpenPlatformMapper openPlatformMapper,
            DayanSecrets dayanSecrets) {
        this.openPlatformMapper = openPlatformMapper;
        this.aesKeyHex = dayanSecrets.aesKeyHex();
    }

    @Override
    public PageResult<SupplierOpenPlatformVO> page(SupplierOpenPlatformQueryDTO query) {
        LambdaQueryWrapper<SupplierOpenPlatform> wrapper = new LambdaQueryWrapper<SupplierOpenPlatform>()
                .eq(query.getSupplierCode() != null && !query.getSupplierCode().isEmpty(),
                        SupplierOpenPlatform::getSupplierCode, query.getSupplierCode())
                .like(query.getPlatformName() != null && !query.getPlatformName().isEmpty(),
                        SupplierOpenPlatform::getPlatformName, query.getPlatformName())
                .eq(query.getProtocolType() != null,
                        SupplierOpenPlatform::getProtocolType, query.getProtocolType())
                .eq(query.getAuthType() != null, SupplierOpenPlatform::getAuthType, query.getAuthType())
                .eq(query.getStatus() != null, SupplierOpenPlatform::getStatus, query.getStatus())
                .orderByDesc(SupplierOpenPlatform::getCreatedAt);
        Page<SupplierOpenPlatform> page = openPlatformMapper.selectPage(
                new Page<>(query.getCurrent(), query.getSize()), wrapper);
        List<SupplierOpenPlatformVO> records = page.getRecords().stream().map(this::toVO).toList();
        return new PageResult<>(query.getCurrent(), query.getSize(), page.getTotal(), records);
    }

    @Override
    public SupplierOpenPlatformVO getDetail(Long id) {
        return toVO(requireById(id));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(SupplierOpenPlatformCreateDTO dto) {
        SupplierOpenPlatform entity = new SupplierOpenPlatform();
        entity.setSupplierCode(dto.getSupplierCode());
        entity.setPlatformName(dto.getPlatformName());
        entity.setApiBaseUrl(dto.getApiBaseUrl());
        entity.setAppKey(dto.getAppKey());
        entity.setAppSecret(encryptSecret(dto.getAppSecret()));
        entity.setCallbackUrl(dto.getCallbackUrl());
        entity.setWebhookSecret(encryptSecret(dto.getWebhookSecret()));
        entity.setProtocolType(dto.getProtocolType());
        entity.setAuthType(dto.getAuthType());
        entity.setDataFormat(dto.getDataFormat());
        entity.setApiVersion(dto.getApiVersion());
        entity.setRateLimit(dto.getRateLimit());
        entity.setTimeout(dto.getTimeout());
        entity.setRetryCount(dto.getRetryCount());
        entity.setExtraConfig(dto.getExtraConfig());
        entity.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
        openPlatformMapper.insert(entity);
        log.info("创建供应商开放平台配置成功: id={}, supplierCode={}, platformName={}",
                entity.getId(), entity.getSupplierCode(), entity.getPlatformName());
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, SupplierOpenPlatformUpdateDTO dto) {
        SupplierOpenPlatform existing = requireById(id);
        SupplierOpenPlatform update = new SupplierOpenPlatform();
        update.setId(existing.getId());
        if (dto.getPlatformName() != null) update.setPlatformName(dto.getPlatformName());
        if (dto.getApiBaseUrl() != null) update.setApiBaseUrl(dto.getApiBaseUrl());
        if (dto.getAppKey() != null) update.setAppKey(dto.getAppKey());
        // appSecret 非空才轮换（避免误清空）
        if (dto.getAppSecret() != null && !dto.getAppSecret().isEmpty()) {
            update.setAppSecret(encryptSecret(dto.getAppSecret()));
        }
        if (dto.getCallbackUrl() != null) update.setCallbackUrl(dto.getCallbackUrl());
        // webhookSecret 非空才轮换
        if (dto.getWebhookSecret() != null && !dto.getWebhookSecret().isEmpty()) {
            update.setWebhookSecret(encryptSecret(dto.getWebhookSecret()));
        }
        if (dto.getProtocolType() != null) update.setProtocolType(dto.getProtocolType());
        if (dto.getAuthType() != null) update.setAuthType(dto.getAuthType());
        if (dto.getDataFormat() != null) update.setDataFormat(dto.getDataFormat());
        if (dto.getApiVersion() != null) update.setApiVersion(dto.getApiVersion());
        if (dto.getRateLimit() != null) update.setRateLimit(dto.getRateLimit());
        if (dto.getTimeout() != null) update.setTimeout(dto.getTimeout());
        if (dto.getRetryCount() != null) update.setRetryCount(dto.getRetryCount());
        if (dto.getExtraConfig() != null) update.setExtraConfig(dto.getExtraConfig());
        if (dto.getStatus() != null) update.setStatus(dto.getStatus());
        openPlatformMapper.updateById(update);
        log.info("更新供应商开放平台配置成功: id={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        requireById(id);
        openPlatformMapper.deleteById(id);
        log.info("删除供应商开放平台配置成功: id={}", id);
    }

    // ====== 内部方法 ======

    private SupplierOpenPlatform requireById(Long id) {
        SupplierOpenPlatform entity = openPlatformMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "供应商开放平台配置不存在: id=" + id);
        }
        return entity;
    }

    private String encryptSecret(String plaintext) {
        if (plaintext == null || plaintext.isEmpty()) {
            return null;
        }
        return AesGcmUtil.encrypt(plaintext, aesKeyHex);
    }

    private SupplierOpenPlatformVO toVO(SupplierOpenPlatform entity) {
        SupplierOpenPlatformVO vo = new SupplierOpenPlatformVO();
        vo.setId(entity.getId());
        vo.setSupplierCode(entity.getSupplierCode());
        vo.setPlatformName(entity.getPlatformName());
        vo.setApiBaseUrl(entity.getApiBaseUrl());
        vo.setAppKey(entity.getAppKey());
        // 密钥脱敏：明文不回传
        vo.setAppSecret(entity.getAppSecret() == null ? null : SECRET_MASK);
        vo.setCallbackUrl(entity.getCallbackUrl());
        vo.setWebhookSecret(entity.getWebhookSecret() == null ? null : SECRET_MASK);
        vo.setProtocolType(entity.getProtocolType());
        vo.setAuthType(entity.getAuthType());
        vo.setDataFormat(entity.getDataFormat());
        vo.setApiVersion(entity.getApiVersion());
        vo.setRateLimit(entity.getRateLimit());
        vo.setTimeout(entity.getTimeout());
        vo.setRetryCount(entity.getRetryCount());
        vo.setExtraConfig(entity.getExtraConfig());
        vo.setStatus(entity.getStatus());
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }
}
