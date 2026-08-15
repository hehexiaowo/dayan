package com.dayan.common.oss.config;

/**
 * 对象存储凭据供应 SPI。
 *
 * <p>默认实现 {@link PropertiesStorageCredentialProvider} 读 Spring 配置
 * （dayan.storage.* / MINIO_* 环境变量）；dayan-module-system 另有
 * {@code @Primary} 的 DB 实现（system_config 表 oss.* 键优先、配置缺失回退本实现），
 * 实现"系统配置页改凭据、各服务热生效"。
 *
 * <p>实现方应自带短 TTL 缓存并保证 {@link #get()} 轻量——
 * {@link DynamicMinioClientHolder} 每次文件操作都会调用做变更对比。
 */
public interface StorageCredentialProvider {

    /** 当前生效的凭据快照（不可为 null，异常时返回 {@link StorageCredential#empty()}） */
    StorageCredential get();
}
