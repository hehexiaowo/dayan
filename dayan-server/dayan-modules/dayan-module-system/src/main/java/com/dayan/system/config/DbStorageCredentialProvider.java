package com.dayan.system.config;

import com.dayan.common.oss.config.PropertiesStorageCredentialProvider;
import com.dayan.common.oss.config.StorageCredential;
import com.dayan.common.oss.config.StorageCredentialProvider;
import com.dayan.system.service.SystemConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * DB 优先的对象存储凭据供应（system_config 表 oss 组）。
 *
 * <p>取值链：system_config（oss.endpoint / oss.bucket / oss.access-key /
 * oss.secret-key / oss.public-base-url）逐键优先 → 缺失键回退 Spring 配置
 * （dayan.storage.* / MINIO_* 环境变量，即 {@link PropertiesStorageCredentialProvider}）。
 * 管理员在系统配置页修改 OSS 凭据后，各服务最迟 REFRESH_MILLIS 内热切换，无需重启。
 *
 * <p>DB 异常（如启动初期连接抖动）整体回退配置值并告警，存储功能不中断。
 */
@Slf4j
@Primary
@Component
@RequiredArgsConstructor
public class DbStorageCredentialProvider implements StorageCredentialProvider {

    /** 凭据快照本地缓存时长（毫秒）：平衡热更时效与文件操作的 DB 查询开销 */
    private static final long REFRESH_MILLIS = 60_000L;

    private final SystemConfigService configService;
    private final PropertiesStorageCredentialProvider propertiesProvider;

    private volatile StorageCredential cached;
    private volatile long cachedAt;

    @Override
    public StorageCredential get() {
        long now = System.currentTimeMillis();
        StorageCredential snapshot = cached;
        if (snapshot != null && now - cachedAt < REFRESH_MILLIS) {
            return snapshot;
        }
        synchronized (this) {
            if (cached != null && System.currentTimeMillis() - cachedAt < REFRESH_MILLIS) {
                return cached;
            }
            StorageCredential merged = load();
            cached = merged;
            cachedAt = System.currentTimeMillis();
            return merged;
        }
    }

    /** DB 逐键读取并与配置兜底合并；DB 不可用时整体回退配置值 */
    private StorageCredential load() {
        StorageCredential fallback = propertiesProvider.get();
        try {
            return new StorageCredential(
                    firstNonBlank(configService.getValue("oss", "oss.endpoint"), fallback.endpoint()),
                    firstNonBlank(configService.getValue("oss", "oss.access-key"), fallback.accessKey()),
                    firstNonBlank(configService.getValue("oss", "oss.secret-key"), fallback.secretKey()),
                    firstNonBlank(configService.getValue("oss", "oss.bucket"), fallback.bucket()),
                    firstNonBlank(configService.getValue("oss", "oss.public-base-url"), fallback.publicBaseUrl()));
        } catch (Exception e) {
            log.warn("system_config 读取 OSS 凭据失败，回退 Spring 配置: {}", e.getMessage());
            return fallback;
        }
    }

    private static String firstNonBlank(String dbValue, String fallbackValue) {
        return dbValue != null && !dbValue.isBlank() ? dbValue : fallbackValue;
    }
}
