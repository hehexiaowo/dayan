package com.dayan.common.oss.config;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 动态 MinioClient 持有器。
 *
 * <p>每次文件操作向 {@link StorageCredentialProvider} 取凭据快照做对比：
 * 未变化返回缓存的 client；变化（系统配置页热改 OSS 凭据）则重建 client
 * 并按需初始化 bucket。凭据供应方自带短 TTL 缓存，本方法的对比开销可忽略。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DynamicMinioClientHolder {

    private final StorageCredentialProvider credentialProvider;

    private volatile StorageCredential cachedCredential;
    private volatile MinioClient cachedClient;

    /** 当前凭据快照（bucket / publicBaseUrl 等随文件操作读取） */
    public StorageCredential credential() {
        return credentialProvider.get();
    }

    /** 获取与当前凭据匹配的 MinioClient（凭据变化时自动重建） */
    public synchronized MinioClient client() {
        StorageCredential current = credentialProvider.get();
        if (!current.isUsable()) {
            throw new IllegalStateException("对象存储凭据不完整（endpoint/accessKey/secretKey/bucket 任一为空），请在系统配置 oss 组或环境变量补齐");
        }
        if (cachedClient != null && current.equals(cachedCredential)) {
            return cachedClient;
        }
        MinioClient client = MinioClient.builder()
                .endpoint(current.endpoint())
                .credentials(current.accessKey(), current.secretKey())
                .build();
        initBucket(client, current.bucket());
        cachedCredential = current;
        cachedClient = client;
        log.info("MinioClient 已按当前凭据构建 endpoint={}, bucket={}", current.endpoint(), current.bucket());
        return client;
    }

    /** 首次构建 / bucket 变更时自动创建 bucket（若不存在）。失败仅告警，上传时再报错。 */
    private void initBucket(MinioClient client, String bucket) {
        try {
            boolean exists = client.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
            if (!exists) {
                client.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
                log.info("MinIO bucket [{}] 创建成功", bucket);
            }
        } catch (Exception e) {
            log.error("MinIO bucket 初始化失败，bucket={}，上传功能将不可用", bucket, e);
        }
    }
}
