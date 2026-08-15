package com.dayan.common.oss.config;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 默认凭据供应实现：直接读 Spring 配置（dayan.storage.*，支持 MINIO_* 环境变量覆盖）。
 *
 * <p>被 system 模块的 DB 优先实现覆盖（@Primary）时，本实现退居回退数据源。
 */
@Component
@RequiredArgsConstructor
public class PropertiesStorageCredentialProvider implements StorageCredentialProvider {

    private final StorageProperties properties;

    @Override
    public StorageCredential get() {
        return new StorageCredential(
                properties.getEndpoint(),
                properties.getAccessKey(),
                properties.getSecretKey(),
                properties.getBucket(),
                properties.getPublicBaseUrl());
    }
}
