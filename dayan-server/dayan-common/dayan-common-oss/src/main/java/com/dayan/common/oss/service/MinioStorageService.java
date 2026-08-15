package com.dayan.common.oss.service;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import com.dayan.common.oss.config.DynamicMinioClientHolder;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.StatObjectArgs;
import io.minio.errors.ErrorResponseException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * MinIO 存储 实现。
 * objectKey 命名规范：{module}/{channelCode}/{yyyy/MM/dd}/{uuid}.{ext}
 *
 * <p>client 与 bucket 均从 {@link DynamicMinioClientHolder} 动态获取
 * （凭据/桶名支持系统配置热更，OSS 凭据收口 system_config）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MinioStorageService implements StorageService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    private final DynamicMinioClientHolder clientHolder;

    @Override
    public String upload(String module, String channelCode, InputStream is, long size,
                         String contentType, String originalName) {
        String ext = FileUtil.extName(originalName);
        String datePath = LocalDate.now().format(DATE_FMT);
        String uuid = IdUtil.simpleUUID();
        String key = module + "/" + channelCode + "/" + datePath + "/" + uuid
                + (ext != null && !ext.isEmpty() ? "." + ext : "");
        MinioClient minioClient = clientHolder.client();
        try {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(clientHolder.credential().bucket())
                            .object(key)
                            .stream(is, size, -1)
                            .contentType(contentType)
                            .build());
            log.info("文件上传成功 key={}, size={}", key, size);
            return key;
        } catch (Exception e) {
            throw new RuntimeException("文件上传失败: " + e.getMessage(), e);
        }
    }

    @Override
    public InputStream download(String key) {
        try {
            return clientHolder.client().getObject(
                    GetObjectArgs.builder()
                            .bucket(clientHolder.credential().bucket())
                            .object(key)
                            .build());
        } catch (Exception e) {
            throw new RuntimeException("文件下载失败 key=" + key + ": " + e.getMessage(), e);
        }
    }

    @Override
    public void delete(String key) {
        try {
            clientHolder.client().removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(clientHolder.credential().bucket())
                            .object(key)
                            .build());
        } catch (Exception e) {
            log.warn("文件删除失败 key={}: {}", key, e.getMessage());
        }
    }

    @Override
    public boolean exists(String key) {
        try {
            clientHolder.client().statObject(
                    StatObjectArgs.builder()
                            .bucket(clientHolder.credential().bucket())
                            .object(key)
                            .build());
            return true;
        } catch (ErrorResponseException e) {
            return false;
        } catch (Exception e) {
            throw new RuntimeException("检查文件存在性失败 key=" + key + ": " + e.getMessage(), e);
        }
    }

    @Override
    public String contentType(String key) {
        String type = FileUtil.getMimeType(key);
        return type != null ? type : "application/octet-stream";
    }
}
