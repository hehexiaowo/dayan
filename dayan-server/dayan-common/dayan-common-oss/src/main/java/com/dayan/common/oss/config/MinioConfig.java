package com.dayan.common.oss.config;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MinIO 客户端配置。启动时自动创建 bucket（若不存在）。
 * 因全仓用 @ComponentScan(basePackages="com.dayan")，此类放本包即可被各 starter 扫到。
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class MinioConfig {

    private final StorageProperties properties;

    @Bean
    public MinioClient minioClient() {
        return MinioClient.builder()
                .endpoint(properties.getEndpoint())
                .credentials(properties.getAccessKey(), properties.getSecretKey())
                .build();
    }

    @PostConstruct
    public void initBucket() {
        try {
            MinioClient client = minioClient();
            boolean exists = client.bucketExists(
                    BucketExistsArgs.builder().bucket(properties.getBucket()).build());
            if (!exists) {
                client.makeBucket(MakeBucketArgs.builder().bucket(properties.getBucket()).build());
                log.info("MinIO bucket [{}] 创建成功", properties.getBucket());
            } else {
                log.info("MinIO bucket [{}] 已存在", properties.getBucket());
            }
        } catch (Exception e) {
            // 建 bucket 失败仅告警，不阻止启动（上传时再报错）
            log.error("MinIO bucket 初始化失败，endpoint={}, bucket={}，上传功能将不可用",
                    properties.getEndpoint(), properties.getBucket(), e);
        }
    }
}
