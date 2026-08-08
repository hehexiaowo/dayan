package com.dayan.common.oss.service;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import com.dayan.common.oss.config.StorageProperties;
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
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MinioStorageService implements StorageService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    private final MinioClient minioClient;
    private final StorageProperties properties;

    @Override
    public String upload(String module, String channelCode, InputStream is, long size,
                         String contentType, String originalName) {
        String ext = FileUtil.extName(originalName);
        String datePath = LocalDate.now().format(DATE_FMT);
        String uuid = IdUtil.simpleUUID();
        String key = module + "/" + channelCode + "/" + datePath + "/" + uuid
                + (ext != null && !ext.isEmpty() ? "." + ext : "");
        try {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(properties.getBucket())
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
            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(properties.getBucket())
                            .object(key)
                            .build());
        } catch (Exception e) {
            throw new RuntimeException("文件下载失败 key=" + key + ": " + e.getMessage(), e);
        }
    }

    @Override
    public void delete(String key) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(properties.getBucket())
                            .object(key)
                            .build());
        } catch (Exception e) {
            log.warn("文件删除失败 key={}: {}", key, e.getMessage());
        }
    }

    @Override
    public boolean exists(String key) {
        try {
            minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(properties.getBucket())
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
