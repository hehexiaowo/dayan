package com.dayan.common.oss.config;

/**
 * 对象存储凭据快照（endpoint / 访问密钥 / bucket / 公网基地址）。
 *
 * <p>值不可变，供 {@link StorageCredentialProvider} 返回、
 * {@link DynamicMinioClientHolder} 做变更对比（凭据变化即重建 MinioClient）。
 */
public record StorageCredential(
        String endpoint,
        String accessKey,
        String secretKey,
        String bucket,
        String publicBaseUrl) {

    /** 兜底空凭据（provider 异常时使用，持有方按值判空跳过） */
    public static StorageCredential empty() {
        return new StorageCredential(null, null, null, null, null);
    }

    public boolean isUsable() {
        return endpoint != null && !endpoint.isEmpty()
                && accessKey != null && !accessKey.isEmpty()
                && secretKey != null && !secretKey.isEmpty()
                && bucket != null && !bucket.isEmpty();
    }
}
