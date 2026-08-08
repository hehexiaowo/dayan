package com.dayan.common.oss.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 文件存储配置（通过 @Value 读取 dayan.storage.* 配置项，遵循全仓配置注入约定）。
 */
@Slf4j
@Component
public class StorageProperties {

    @Value("${dayan.storage.endpoint:http://localhost:9000}")
    private String endpoint;

    @Value("${dayan.storage.access-key:dayan}")
    private String accessKey;

    @Value("${dayan.storage.secret-key:dayan12345}")
    private String secretKey;

    @Value("${dayan.storage.bucket:dayan-public}")
    private String bucket;

    @Value("${dayan.storage.max-size:10485760}")
    private long maxSize;

    public String getEndpoint() { return endpoint; }
    public String getAccessKey() { return accessKey; }
    public String getSecretKey() { return secretKey; }
    public String getBucket() { return bucket; }
    public long getMaxSize() { return maxSize; }
}
