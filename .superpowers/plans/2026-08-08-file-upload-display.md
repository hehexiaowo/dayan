# 文件上传与显示 实现计划

> **面向 AI 代理的工作者：** 必需子技能：使用 superpowers:subagent-driven-development（推荐）或 superpowers:executing-plans 逐任务实现此计划。步骤使用复选框（`- [ ]`）语法来跟踪进度。

**目标：** 建立 MinIO 文件存储能力，admin 端 41 处手填 URL 输入框替换为 `<FileUploader>` 组件，上传/显示全链路打通，channel 端修复只读展示。

**架构：** 三层——MinIO 存储（docker-compose）+ `dayan-common-oss` 的 `StorageService` 抽象（MinIO 实现）+ 各端 `FileController`（admin 有 upload+preview，channel 只 preview）。上传走后端流式中转，显示走后端代理下载（同源零 CORS）。DB 存纯 key，前端 `formatFileUrl` 拼前缀且兼容历史 http 数据。

**技术栈：** Java 21 / Spring Boot 3.2.8 / MinIO SDK (`io.minio:minio`) / Hutool 5.8.29 / Sa-Token 1.39 / Vue 3.4 + element-plus 2.7.8 + Vite 5 + TypeScript

**设计文档：** `.superpowers/specs/2026-08-08-file-upload-display-design.md`

**关键勘察事实（影响实现细节）：**
- 统一返回类 `R<T>`（`com.dayan.common.core.resp.R`），成功 `R.ok(data)`，失败 `R.fail(code,msg)`
- 异常体系：`BusinessException(ErrorCode.BUSINESS, "msg")` 被 `GlobalExceptionHandler` 自动转 `R.fail`，Controller 无需 try-catch
- 配置注入全仓用 `@Value("${dayan.xxx:default}")`，**不用** `@ConfigurationProperties`
- 自动装配靠 `@ComponentScan(basePackages="com.dayan")`，config 类放 `com.dayan.common.oss.config` 即可，**无需** spring.factories
- Controller 注入风格：`@RequiredArgsConstructor` + `private final XxxService`
- Controller 包路径约定：`com.dayan.{module}.controller.{end}`（端隔离靠包路径 + 启动类 excludeFilters 正则）
- 全仓**无** `MultipartFile` 使用，本计划是首个上传接口
- 前端 request 封装：`request<T>({url,method,data})`，URL 以 `/admin-api/` 开头走 Vite proxy
- 前端组件风格：`<script setup lang="ts">` + `defineProps` 泛型 + `defineEmits`，v-model 用 `:model-value` + `@update:model-value`
- `imageUrls` 字段是 **string**（逗号分隔或 JSON），多图组件需 string↔array 转换
- admin starter pom 第 93 行、channel starter pom 第 77 行是 common 依赖区，需加 `dayan-common-oss`

---

## 文件结构

### 新建文件

**后端 — dayan-common-oss 模块（存储抽象层）：**
| 文件 | 职责 |
|---|---|
| `dayan-server/dayan-common/dayan-common-oss/pom.xml`（改） | 加 minio SDK 依赖 |
| `.../common/oss/config/StorageProperties.java` | `@Value("${dayan.storage.*}")` 读配置 |
| `.../common/oss/config/MinioConfig.java` | `@Bean MinioClient` + 启动建 bucket |
| `.../common/oss/service/StorageService.java` | 接口：upload/download/delete/exists/contentType |
| `.../common/oss/service/MinioStorageService.java` | MinIO 实现 |
| `.../common/oss/dto/FileUploadDTO.java` | 上传返回结构 |

**后端 — admin 端 FileController（放 dayan-module-system）：**
| 文件 | 职责 |
|---|---|
| `dayan-module-system/.../controller/admin/FileAdminController.java` | `POST /upload` + `GET /preview/**` |

**后端 — channel 端 FileController（放 dayan-module-channel）：**
| 文件 | 职责 |
|---|---|
| `dayan-module-channel/.../controller/channel/FileChannelController.java` | `GET /preview/**`（只读） |

**前端 — admin：**
| 文件 | 职责 |
|---|---|
| `dayan-admin/src/components/FileUploader/index.vue` | 通用上传组件 |
| `dayan-admin/src/api/file.ts` | `uploadFile()` API |
| `dayan-admin/src/utils/file.ts` | `formatFileUrl(key)` 工具 |

**前端 — channel：**
| 文件 | 职责 |
|---|---|
| `dayan-channel/src/utils/file.ts` | `formatFileUrl(key)` 工具 |

**基础设施：**
| 文件 | 职责 |
|---|---|
| `docker-compose.infra.yml`（改） | 加 dayan-minio 服务 |

### 修改文件（配置 + 41 处替换）
- admin/channel starter 的 pom.xml + application.yml
- admin 前端 25 个 .vue 的 41 处 el-input
- channel 前端 3-4 个 .vue 的展示位

---

## 任务 1：docker-compose 加 MinIO

**文件：**
- 修改：`docker-compose.infra.yml`

- [ ] **步骤 1：在 services 段加 dayan-minio 服务**

在 `docker-compose.infra.yml` 的 services 段末尾（nacos 服务之后）加：

```yaml
  dayan-minio:
    image: minio/minio:latest
    container_name: dayan-minio
    restart: unless-stopped
    ports:
      - "9000:9000"
      - "9001:9001"
    environment:
      MINIO_ROOT_USER: dayan
      MINIO_ROOT_PASSWORD: dayan12345
    volumes:
      - minio-data:/data
    command: server /data --console-address ":9001"
```

- [ ] **步骤 2：在 volumes 段加 minio-data**

在文件底部的 `volumes:` 段加 `minio-data:`（与 mysql-data、redis-data 平级）：

```yaml
volumes:
  mysql-data:
  redis-data:
  nacos-data:
  minio-data:
```

- [ ] **步骤 3：启动 MinIO 并验证**

运行：`docker compose -f docker-compose.infra.yml up -d dayan-minio`
预期：容器启动，`docker ps` 看到 dayan-minio

验证 console：浏览器打开 `http://localhost:9001`，用 `dayan` / `dayan12345` 登录，能看到空 bucket 列表。

- [ ] **步骤 4：Commit**

```bash
git add docker-compose.infra.yml
git commit -m "infra: docker-compose 加 dayan-minio 服务(9000 API/9001 Console)"
```

---

## 任务 2：dayan-common-oss 实装存储层

**文件：**
- 修改：`dayan-server/dayan-common/dayan-common-oss/pom.xml`
- 创建：`dayan-server/dayan-common/dayan-common-oss/src/main/java/com/dayan/common/oss/config/StorageProperties.java`
- 创建：`dayan-server/dayan-common/dayan-common-oss/src/main/java/com/dayan/common/oss/config/MinioConfig.java`
- 创建：`dayan-server/dayan-common/dayan-common-oss/src/main/java/com/dayan/common/oss/service/StorageService.java`
- 创建：`dayan-server/dayan-common/dayan-common-oss/src/main/java/com/dayan/common/oss/service/MinioStorageService.java`
- 创建：`dayan-server/dayan-common/dayan-common-oss/src/main/java/com/dayan/common/oss/dto/FileUploadDTO.java`

- [ ] **步骤 1：pom.xml 加 MinIO SDK 依赖**

在 `dayan-common-oss/pom.xml` 的 `<dependencies>` 段加（在 lombok 之前，保持字母序）：

```xml
        <dependency>
            <groupId>io.minio</groupId>
            <artifactId>minio</artifactId>
            <version>8.5.12</version>
        </dependency>
```

- [ ] **步骤 2：创建 StorageProperties**

`config/StorageProperties.java`：

```java
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
```

- [ ] **步骤 3：创建 MinioConfig（Bean + 启动建 bucket）**

`config/MinioConfig.java`：

```java
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
```

- [ ] **步骤 4：创建 StorageService 接口**

`service/StorageService.java`：

```java
package com.dayan.common.oss.service;

import java.io.InputStream;

/**
 * 文件存储抽象接口。当前实现 MinioStorageService；
 * 预留未来 OSS/COS 实现，以及预签名直传扩展（接口不变，加 presign 方法即可）。
 */
public interface StorageService {

    /**
     * 上传文件，返回生成的 objectKey。
     *
     * @param module        业务模块（goods/scene/park 等）
     * @param channelCode   渠道编码
     * @param is            文件输入流
     * @param size          文件大小（字节）
     * @param contentType   MIME 类型
     * @param originalName  原始文件名（用于提取后缀，不保留原名）
     * @return objectKey，如 goods/day001/2026/08/08/abc.jpg
     */
    String upload(String module, String channelCode, InputStream is, long size,
                  String contentType, String originalName);

    /** 下载文件，返回输入流（调用方负责 close）。 */
    InputStream download(String key);

    /** 删除文件。 */
    void delete(String key);

    /** 判断文件是否存在。 */
    boolean exists(String key);

    /** 根据 key 推断 Content-Type。 */
    String contentType(String key);
}
```

- [ ] **步骤 5：创建 MinioStorageService 实现**

`service/MinioStorageService.java`：

```java
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
```

- [ ] **步骤 6：创建 FileUploadDTO**

`dto/FileUploadDTO.java`：

```java
package com.dayan.common.oss.dto;

import lombok.Data;

/**
 * 文件上传返回结构。
 * url 用于上传后即时预览；key 是 DB 持久化值（端无关/环境无关）。
 */
@Data
public class FileUploadDTO {
    /** 访问 URL，如 /admin-api/v1/files/preview/{key} */
    private String url;
    /** 对象 key，如 goods/day001/2026/08/08/abc.jpg（存入 DB） */
    private String key;
    /** 原始文件名 */
    private String originalName;
    /** 文件大小（字节） */
    private long size;
}
```

- [ ] **步骤 7：编译验证 dayan-common-oss**

运行：`cd dayan-server && mvn install -pl dayan-common/dayan-common-oss -am -q -DskipTests`
预期：BUILD SUCCESS（安装到本地 .m2，后续 starter 模块才能引用）

- [ ] **步骤 8：Commit**

```bash
git add dayan-server/dayan-common/dayan-common-oss
git commit -m "feat(oss): dayan-common-oss 实装 MinIO 存储层(StorageService+MinioConfig+bucket自动建)"
```

---

## 任务 3：admin 端接入 oss 模块 + FileController

**文件：**
- 修改：`dayan-server/dayan-starters/dayan-admin/pom.xml`（第 93 行后加依赖）
- 修改：`dayan-server/dayan-starters/dayan-admin/src/main/resources/application.yml`
- 创建：`dayan-server/dayan-modules/dayan-module-system/src/main/java/com/dayan/system/controller/admin/FileAdminController.java`

- [ ] **步骤 1：admin starter pom 加 dayan-common-oss 依赖**

在 `dayan-admin/pom.xml` 第 93 行（dayan-common-swagger 依赖之后）加：

```xml
        <dependency>
            <groupId>com.dayan</groupId>
            <artifactId>dayan-common-oss</artifactId>
        </dependency>
```

（无需 version，dayan-common-bom 已管理）

- [ ] **步骤 2：application.yml 加 storage + multipart 配置**

在 `dayan-admin/src/main/resources/application.yml` 加两段（与 `dayan.security` 平级）：

```yaml
spring:
  servlet:
    multipart:
      max-file-size: 10MB
      max-request-size: 20MB

dayan:
  storage:
    endpoint: http://localhost:9000
    access-key: dayan
    secret-key: dayan12345
    bucket: dayan-public
    max-size: 10485760
```

注意：若 yml 中已有 `spring:` 顶级 key，把 `servlet` 并入已有 spring 段；`dayan:` 同理并入已有 dayan 段。

- [ ] **步骤 3：创建 FileAdminController（upload + preview）**

`dayan-module-system/src/main/java/com/dayan/system/controller/admin/FileAdminController.java`：

```java
package com.dayan.system.controller.admin;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.StrUtil;
import com.dayan.common.core.exception.BusinessException;
import com.dayan.common.core.exception.ErrorCode;
import com.dayan.common.core.resp.R;
import com.dayan.common.oss.config.StorageProperties;
import com.dayan.common.oss.dto.FileUploadDTO;
import com.dayan.common.oss.service.StorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;

/**
 * 文件上传/预览 Controller（admin 端）。
 * - POST /v1/files/upload   上传，返回 key（存 DB）
 * - GET  /v1/files/preview/**  代理下载（同源零 CORS）
 */
@Tag(name = "文件管理")
@RestController
@RequestMapping("/v1/files")
@RequiredArgsConstructor
@Slf4j
public class FileAdminController {

    private final StorageService storageService;
    private final StorageProperties storageProperties;

    /** 允许的文件后缀白名单 */
    private static final Set<String> ALLOWED_EXT = Set.of(
            "jpg", "jpeg", "png", "gif", "webp",
            "mp4", "webm",
            "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt");

    @Operation(summary = "上传文件")
    @PostMapping("/upload")
    public R<FileUploadDTO> upload(@RequestParam("file") MultipartFile file,
                                   @RequestParam(value = "module", required = false) String module) {
        if (file.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "文件不能为空");
        }
        long size = file.getSize();
        if (size > storageProperties.getMaxSize()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR,
                    "文件大小超过限制 " + (storageProperties.getMaxSize() / 1024 / 1024) + "MB");
        }
        String originalName = file.getOriginalFilename();
        String ext = cn.hutool.core.io.FileUtil.extName(originalName);
        if (StrUtil.isBlank(ext) || !ALLOWED_EXT.contains(ext.toLowerCase())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "不支持的文件类型: " + ext);
        }
        String channelCode = "common";
        try {
            // 从 Sa-Token 获取当前渠道编码（若登录上下文有）
            channelCode = (String) StpUtil.getSession().get("channelCode");
            if (StrUtil.isBlank(channelCode)) {
                channelCode = "admin";
            }
        } catch (Exception ignored) {
            // 未登录或无 session 时用默认值
        }
        String mod = StrUtil.isBlank(module) ? "common" : module;
        try {
            String key = storageService.upload(mod, channelCode,
                    file.getInputStream(), size, file.getContentType(), originalName);
            FileUploadDTO dto = new FileUploadDTO();
            dto.setKey(key);
            dto.setUrl("/admin-api/v1/files/preview/" + key);
            dto.setOriginalName(originalName);
            dto.setSize(size);
            return R.ok(dto);
        } catch (Exception e) {
            log.error("文件上传失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "文件上传失败: " + e.getMessage());
        }
    }

    @Operation(summary = "预览/下载文件（代理下载）")
    @GetMapping("/preview/**")
    public void preview(HttpServletRequest request, HttpServletResponse response) {
        // key 含斜杠（如 goods/day001/2026/08/08/abc.jpg），从 URI 提取 preview/ 之后的部分
        String uri = request.getRequestURI();
        String prefix = "/admin-api/v1/files/preview/";
        String key = uri.substring(uri.indexOf(prefix) + prefix.length());
        if (StrUtil.isBlank(key)) {
            response.setStatus(404);
            return;
        }
        if (!storageService.exists(key)) {
            response.setStatus(404);
            return;
        }
        response.setContentType(storageService.contentType(key));
        response.setHeader("Cache-Control", "max-age=86400");
        try (InputStream is = storageService.download(key);
             OutputStream os = response.getOutputStream()) {
            is.transferTo(os);
            os.flush();
        } catch (Exception e) {
            log.error("文件预览失败 key={}", key, e);
            response.setStatus(500);
        }
    }
}
```

- [ ] **步骤 4：编译 admin 模块**

运行：`cd dayan-server && mvn install -pl dayan-starters/dayan-admin -am -q -DskipTests`
预期：BUILD SUCCESS

- [ ] **步骤 5：启动 admin 并验证上传接口**

启动：`cd dayan-server && mvn spring-boot:run -pl dayan-starters/dayan-admin`（后台运行）

先登录拿 token，再用 curl 上传测试：

```bash
# 上传测试图（先准备一个 test.jpg）
curl -X POST http://localhost:8080/admin-api/v1/files/upload \
  -H "Admin-Token: <token>" \
  -F "file=@test.jpg" \
  -F "module=goods"
```
预期：返回 `{"code":0,"data":{"url":"/admin-api/v1/files/preview/goods/...","key":"goods/...","originalName":"test.jpg","size":...}}`

验证预览：浏览器或 curl 访问 `http://localhost:8080/admin-api/v1/files/preview/{返回的key}`，应返回图片二进制。

- [ ] **步骤 6：Commit**

```bash
git add dayan-server/dayan-starters/dayan-admin dayan-server/dayan-modules/dayan-module-system
git commit -m "feat(admin): 文件上传+代理预览接口 /v1/files/upload + /v1/files/preview/**"
```

---

## 任务 4：channel 端接入 oss + 只读 preview Controller

**文件：**
- 修改：`dayan-server/dayan-starters/dayan-channel/pom.xml`（第 77 行后加依赖）
- 修改：`dayan-server/dayan-starters/dayan-channel/src/main/resources/application.yml`
- 创建：`dayan-server/dayan-modules/dayan-module-channel/src/main/java/com/dayan/channel/controller/channel/FileChannelController.java`

- [ ] **步骤 1：channel starter pom 加 dayan-common-oss 依赖**

在 `dayan-channel/pom.xml` 第 77 行后加：

```xml
        <dependency>
            <groupId>com.dayan</groupId>
            <artifactId>dayan-common-oss</artifactId>
        </dependency>
```

- [ ] **步骤 2：channel application.yml 加配置**

同任务 3 步骤 2，在 `dayan-channel/src/main/resources/application.yml` 加 `spring.servlet.multipart` + `dayan.storage.*`。

- [ ] **步骤 3：创建 FileChannelController（只读 preview）**

`dayan-module-channel/src/main/java/com/dayan/channel/controller/channel/FileChannelController.java`：

```java
package com.dayan.channel.controller.channel;

import cn.hutool.core.util.StrUtil;
import com.dayan.common.oss.service.StorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * 文件预览 Controller（channel 端，只读）。
 * channel 端本轮不提供上传，只复用同一 MinIO 做代理下载显示。
 */
@Tag(name = "文件预览")
@RestController
@RequestMapping("/v1/files")
@RequiredArgsConstructor
@Slf4j
public class FileChannelController {

    private final StorageService storageService;

    @Operation(summary = "预览/下载文件（代理下载）")
    @GetMapping("/preview/**")
    public void preview(HttpServletRequest request, HttpServletResponse response) {
        String uri = request.getRequestURI();
        String prefix = "/channel-api/v1/files/preview/";
        String key = uri.substring(uri.indexOf(prefix) + prefix.length());
        if (StrUtil.isBlank(key) || !storageService.exists(key)) {
            response.setStatus(404);
            return;
        }
        response.setContentType(storageService.contentType(key));
        response.setHeader("Cache-Control", "max-age=86400");
        try (InputStream is = storageService.download(key);
             OutputStream os = response.getOutputStream()) {
            is.transferTo(os);
            os.flush();
        } catch (Exception e) {
            log.error("文件预览失败 key={}", key, e);
            response.setStatus(500);
        }
    }
}
```

- [ ] **步骤 4：编译并启动 channel 验证 preview**

运行：`cd dayan-server && mvn install -pl dayan-starters/dayan-channel -am -q -DskipTests`
预期：BUILD SUCCESS

启动 channel，用 admin 上传的 key 测试 channel preview：
```bash
curl -I http://localhost:8081/channel-api/v1/files/preview/goods/.../xxx.jpg
```
预期：HTTP 200 + Content-Type: image/jpeg

- [ ] **步骤 5：Commit**

```bash
git add dayan-server/dayan-starters/dayan-channel dayan-server/dayan-modules/dayan-module-channel
git commit -m "feat(channel): 文件只读代理预览接口 /v1/files/preview/**"
```

---

## 任务 5：admin 前端 FileUploader 组件 + API + 工具函数

**文件：**
- 创建：`dayan-admin/src/api/file.ts`
- 创建：`dayan-admin/src/utils/file.ts`
- 创建：`dayan-admin/src/components/FileUploader/index.vue`

- [ ] **步骤 1：创建 api/file.ts**

`dayan-admin/src/api/file.ts`：

```typescript
import { request } from '@/utils/request'

/** 文件上传返回结构 */
export interface FileUploadDTO {
  url: string
  key: string
  originalName: string
  size: number
}

/** 上传文件：POST /admin-api/v1/files/upload */
export function uploadFile(file: File, module?: string): Promise<FileUploadDTO> {
  const formData = new FormData()
  formData.append('file', file)
  if (module) formData.append('module', module)
  return request<FileUploadDTO>({
    url: '/admin-api/v1/files/upload',
    method: 'post',
    data: formData,
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 60000
  })
}
```

- [ ] **步骤 2：创建 utils/file.ts（formatFileUrl 工具）**

`dayan-admin/src/utils/file.ts`：

```typescript
/**
 * 将 DB 中存的 key（或历史 http URL）转为可访问的 URL。
 * - http/https 开头的值（历史手填数据）：原样返回，兼容旧数据
 * - 纯 key（如 goods/day001/2026/08/08/abc.jpg）：拼接 admin preview 前缀
 * - 空值：返回空字符串
 */
export function formatFileUrl(value: string | undefined | null): string {
  if (!value) return ''
  if (value.startsWith('http://') || value.startsWith('https://')) return value
  return `/admin-api/v1/files/preview/${value}`
}
```

- [ ] **步骤 3：创建 FileUploader 组件**

`dayan-admin/src/components/FileUploader/index.vue`：

```vue
<script setup lang="ts">
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus, VideoPlay, Document, Delete } from '@element-plus/icons-vue'
import { uploadFile } from '@/api/file'
import { formatFileUrl } from '@/utils/file'

defineOptions({ name: 'FileUploader' })

type FileType = 'image' | 'video' | 'file' | 'vr' | 'any'

const props = withDefaults(defineProps<{
  type?: FileType
  multiple?: boolean
  modelValue?: string | string[]
  accept?: string
  maxSize?: number // MB
  limit?: number
  disabled?: boolean
  module?: string
}>(), {
  type: 'any',
  multiple: false,
  modelValue: '',
  maxSize: 10,
  limit: 9,
  disabled: false,
  module: 'common'
})

const emit = defineEmits<{
  (e: 'update:modelValue', v: string | string[]): void
}>()

const uploading = ref(false)

const defaultAccept = computed(() => {
  switch (props.type) {
    case 'image': return 'image/jpeg,image/png,image/gif,image/webp'
    case 'video': return 'video/mp4,video/webm'
    case 'file': return '.pdf,.doc,.docx,.xls,.xlsx,.ppt,.pptx,.txt'
    default: return ''
  }
})

const acceptVal = computed(() => props.accept || defaultAccept.value)

// 单文件模式：返回 string
const singleValue = computed(() => {
  if (props.multiple) return ''
  return typeof props.modelValue === 'string' ? props.modelValue : ''
})

// 多文件模式：返回 string[]
const multiValue = computed(() => {
  if (!props.multiple) return []
  if (Array.isArray(props.modelValue)) return props.modelValue
  return []
})

async function handleUpload(e: Event) {
  const input = e.target as HTMLInputElement
  if (!input.files || input.files.length === 0) return
  const file = input.files[0]
  // 前端预校验大小
  if (file.size > props.maxSize * 1024 * 1024) {
    ElMessage.error(`文件大小不能超过 ${props.maxSize}MB`)
    input.value = ''
    return
  }
  uploading.value = true
  try {
    const res = await uploadFile(file, props.module)
    if (props.multiple) {
      const arr = [...multiValue.value, res.key]
      emit('update:modelValue', arr)
    } else {
      emit('update:modelValue', res.key)
    }
    ElMessage.success('上传成功')
  } catch {
    // request 拦截器已弹错误 toast
  } finally {
    uploading.value = false
    input.value = ''
  }
}

function removeSingle() {
  emit('update:modelValue', '')
}

function removeMulti(index: number) {
  const arr = multiValue.value.filter((_, i) => i !== index)
  emit('update:modelValue', arr)
}

function fileName(key: string): string {
  const parts = key.split('/')
  return parts[parts.length - 1]
}
</script>

<template>
  <!-- 单图 -->
  <div v-if="type === 'image' && !multiple" class="uploader-single-image">
    <div v-if="singleValue" class="image-preview">
      <el-image :src="formatFileUrl(singleValue)" fit="cover" class="preview-img" :preview-src-list="[formatFileUrl(singleValue)]" />
      <div v-if="!disabled" class="image-actions">
        <label class="action-btn">替换
          <input type="file" :accept="acceptVal" class="hidden-input" @change="handleUpload" :disabled="uploading" />
        </label>
        <el-icon class="action-btn" @click="removeSingle"><Delete /></el-icon>
      </div>
    </div>
    <label v-else class="upload-placeholder" :class="{ disabled }">
      <el-icon><Plus /></el-icon>
      <span>{{ uploading ? '上传中...' : '上传图片' }}</span>
      <input type="file" :accept="acceptVal" class="hidden-input" @change="handleUpload" :disabled="uploading || disabled" />
    </label>
  </div>

  <!-- 多图 -->
  <div v-else-if="type === 'image' && multiple" class="uploader-multi-image">
    <div v-for="(key, i) in multiValue" :key="i" class="image-preview">
      <el-image :src="formatFileUrl(key)" fit="cover" class="preview-img" :preview-src-list="multiValue.map(formatFileUrl)" :initial-index="i" />
      <div v-if="!disabled" class="image-actions">
        <el-icon class="action-btn" @click="removeMulti(i)"><Delete /></el-icon>
      </div>
    </div>
    <label v-if="!disabled && multiValue.length < limit" class="upload-placeholder" :class="{ disabled }">
      <el-icon><Plus /></el-icon>
      <span>{{ uploading ? '上传中...' : '添加图片' }}</span>
      <input type="file" :accept="acceptVal" class="hidden-input" @change="handleUpload" :disabled="uploading || disabled" />
    </label>
  </div>

  <!-- 视频 -->
  <div v-else-if="type === 'video'" class="uploader-media">
    <div v-if="singleValue" class="media-item">
      <el-icon class="media-icon"><VideoPlay /></el-icon>
      <span class="media-name">{{ fileName(singleValue) }}</span>
      <el-icon v-if="!disabled" class="action-btn" @click="removeSingle"><Delete /></el-icon>
    </div>
    <label v-else class="upload-btn" :class="{ disabled }">
      <el-icon><Plus /></el-icon><span>{{ uploading ? '上传中...' : '上传视频' }}</span>
      <input type="file" :accept="acceptVal" class="hidden-input" @change="handleUpload" :disabled="uploading || disabled" />
    </label>
  </div>

  <!-- 文件 / VR / any -->
  <div v-else class="uploader-media">
    <div v-if="!multiple && singleValue" class="media-item">
      <el-icon class="media-icon"><Document /></el-icon>
      <span class="media-name">{{ fileName(singleValue) }}</span>
      <el-icon v-if="!disabled" class="action-btn" @click="removeSingle"><Delete /></el-icon>
    </div>
    <div v-for="(key, i) in (multiple ? multiValue : [])" :key="i" class="media-item">
      <el-icon class="media-icon"><Document /></el-icon>
      <span class="media-name">{{ fileName(key) }}</span>
      <el-icon v-if="!disabled" class="action-btn" @click="removeMulti(i)"><Delete /></el-icon>
    </div>
    <label v-if="!disabled && (!multiple || multiValue.length < limit)" class="upload-btn" :class="{ disabled }">
      <el-icon><Plus /></el-icon><span>{{ uploading ? '上传中...' : '点击上传' }}</span>
      <input type="file" :accept="acceptVal" class="hidden-input" @change="handleUpload" :disabled="uploading || disabled" />
    </label>
  </div>
</template>

<style scoped>
.hidden-input { display: none; }
.upload-placeholder, .upload-btn, .action-btn { cursor: pointer; }
.upload-placeholder.disabled, .upload-btn.disabled { cursor: not-allowed; opacity: 0.5; }
.upload-placeholder {
  display: flex; flex-direction: column; align-items: center; justify-content: center;
  width: 80px; height: 80px; border: 1px dashed #d9d9d9; border-radius: 6px;
  color: #999; font-size: 12px; gap: 4px;
}
.upload-placeholder:hover { border-color: #409eff; color: #409eff; }
.image-preview { position: relative; width: 80px; height: 80px; }
.preview-img { width: 80px; height: 80px; border-radius: 6px; border: 1px solid #ebeef5; }
.image-actions {
  position: absolute; inset: 0; display: flex; align-items: center; justify-content: center;
  gap: 8px; background: rgba(0,0,0,0.5); border-radius: 6px; opacity: 0; transition: opacity 0.2s;
}
.image-preview:hover .image-actions { opacity: 1; }
.action-btn { color: #fff; font-size: 16px; }
.uploader-multi-image, .uploader-single-image, .uploader-media { display: flex; flex-wrap: wrap; gap: 8px; align-items: center; }
.media-item { display: flex; align-items: center; gap: 6px; padding: 4px 8px; border: 1px solid #ebeef5; border-radius: 4px; font-size: 13px; }
.media-icon { color: #409eff; }
.media-name { max-width: 160px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.upload-btn { display: inline-flex; align-items: center; gap: 4px; color: #409eff; font-size: 13px; }
</style>
```

- [ ] **步骤 4：Commit**

```bash
git add dayan-admin/src/components/FileUploader dayan-admin/src/api/file.ts dayan-admin/src/utils/file.ts
git commit -m "feat(admin-ui): 通用 FileUploader 组件 + 上传API + formatFileUrl 工具"
```

---

## 任务 6：admin 前端单页接入验证（MediaImagePane）

先用一个页面打通整条链路（上传→存 key→显示），验证通过后再批量替换。

**文件：**
- 修改：`dayan-admin/src/views/resource/park/detail/MediaImagePane.vue`（第 253 行 el-input 替换）
- 修改：`dayan-admin/src/views/resource/park/detail/MediaImagePane.vue`（第 184-186 行 el-image 用 formatFileUrl）

- [ ] **步骤 1：替换 MediaImagePane 的 imageUrl 输入框**

打开 `MediaImagePane.vue`，找到第 251-255 行的 `el-form-item`，把内部 `el-input` 替换为 `<FileUploader>`：

原代码：
```vue
          <el-form-item label="图片URL" prop="imageUrl">
            <el-input v-model="form.imageUrl" placeholder="图片 URL（必填）" />
          </el-form-item>
```

改为：
```vue
          <el-form-item label="图片" prop="imageUrl">
            <FileUploader v-model="form.imageUrl" type="image" module="park" />
          </el-form-item>
```

- [ ] **步骤 2：import 组件**

在 `<script setup>` 段顶部加 import（与其他 import 平级）：

```typescript
import FileUploader from '@/components/FileUploader/index.vue'
```

- [ ] **步骤 3：表格展示列用 formatFileUrl**

找到第 184-186 行的 `<el-image :src="row.imageUrl"`，改为 `:src="formatFileUrl(row.imageUrl)"`。

在 script 段 import：
```typescript
import { formatFileUrl } from '@/utils/file'
```

- [ ] **步骤 4：GUI 验证整条链路**

启动 admin 前端（`cd dayan-admin && npm run dev`），后端已起。

1. 进入 资源 → 机构 → 媒体 → 图片库 Tab
2. 新建：点上传图片，选一张图，应看到缩略图预览
3. 保存 → 列表应显示该图缩略图（el-image 正常渲染）
4. 编辑该条 → 缩略图应回显
5. 删除图片 → 重新上传另一张 → 保存

预期：上传成功、缩略图显示正常、刷新后回显正常、不报 CORS 错误。

- [ ] **步骤 5：Commit**

```bash
git add dayan-admin/src/views/resource/park/detail/MediaImagePane.vue
git commit -m "feat(admin-ui): MediaImagePane 接入 FileUploader 验证上传全链路"
```

---

## 任务 7：admin 前端批量替换单图字段（18 处）

所有 `type=image, single` 的字段。模式统一：`<el-input v-model="form.xxx">` → `<FileUploader v-model="form.xxx" type="image" module="...">`，展示位 `<el-image :src="row.xxx">` → `:src="formatFileUrl(row.xxx)"`。

**文件清单（每个文件都要加 import FileUploader + import formatFileUrl）：**

- [ ] **步骤 1：goods/info/index.vue + detail/BasicTab.vue（coverImage）**

`goods/info/index.vue` 第 535-539 行（封面图 el-input）替换为：
```vue
            <FileUploader v-model="form.coverImage" type="image" module="goods" />
```

`goods/info/detail/BasicTab.vue` 第 290 行 coverImage 同样替换。

- [ ] **步骤 2：goods/info/index.vue + detail/BasicTab.vue 展示位**

若有展示 coverImage 的 `<el-image>` 或 descriptions item，用 formatFileUrl 包裹。检查 detail/BasicTab.vue 第 184-185 行。

- [ ] **步骤 3：scene/index.vue + scene/detail/BasicTab.vue（coverImage）**

`resource/scene/index.vue` 第 592 行 coverImage 替换；`scene/detail/BasicTab.vue` 第 268 行替换；展示位第 190 行 formatFileUrl。

- [ ] **步骤 4：park 子表（FoodTab/Facility/Adviser/Room/ServiceItem coverImage 系列）**

- `resource/park/detail/FoodTab.vue` 第 502 行 `typeForm.coverImage`
- `resource/park/detail/FacilityTab.vue` 第 324 行 `form.coverImage`
- `resource/park/detail/AdviserTab.vue` 第 264 行 `form.adviserImage`
- `resource/park/detail/RoomTab.vue` 第 629 行 `typeForm.coverImage`（type=image）、第 634 行 `typeForm.designImage`（type=image）
- `resource/park/detail/ServiceItemPane.vue` 第 308 行 `form.coverImage`

每处替换为对应 `<FileUploader v-model="..." type="image" module="park" />`。

- [ ] **步骤 5：MediaVideoPane（coverUrl）+ MediaVrPane（thumbnailUrl）**

- `resource/park/detail/MediaVideoPane.vue` 第 246 行 `form.coverUrl`（type=image, module=park）
- `resource/park/detail/MediaVrPane.vue` 第 260 行 `form.thumbnailUrl`（type=image, module=park）

- [ ] **步骤 6：avatar 系列（channel/agent/client/butler）**

- `channel/info/detail/AccountTab.vue` 第 359 行 `form.avatar`（type=image, module=channel）
- `channel/info/index.vue` 第 503 行 `form.logoUrl`（type=image, module=channel）— 注意这是 logoUrl
- `channel/agent/index.vue` 第 415 行 `form.avatar`
- `channel/client/index.vue` 第 147 行 `form.avatar`
- `service/butler/index.vue` 第 310 行 `form.avatar`（module=service）
- `service/butler/detail/BasicTab.vue` 第 180 行 `form.avatar`；展示位第 136 行 formatFileUrl

- [ ] **步骤 7：supplier/logoUrl + course/coverImage + content/coverImage + equity 系列**

- `resource/supplier/index.vue` 第 543 行 `form.logoUrl`（type=image, module=supplier）
- `resource/course/index.vue` 第 336 行 `form.coverImage`（module=course）
- `resource/content/index.vue` 第 458 行 `form.coverImage`（module=content）
- `equity/template/index.vue` 第 447 行 `form.coverImage`（module=equity）、第 452 行 `form.cardDesignUrl`（type=image, module=equity）

- [ ] **步骤 8：channel info logoUrl detail BasicTab**

- `channel/info/detail/BasicTab.vue` 第 300 行 `form.logoUrl`（type=image, module=channel）；展示位第 184-185 行 formatFileUrl

- [ ] **步骤 9：GUI 抽查 + Commit**

每个模块至少抽查 1 个页面：上传 → 保存 → 刷新 → 显示。全部通过后：

```bash
git add dayan-admin/src/views
git commit -m "feat(admin-ui): 18 处单图字段接入 FileUploader(cover/logo/avatar/design/card等)"
```

---

## 任务 8：admin 前端批量替换多图字段（8 处，string↔array 转换）

多图字段（`imageUrls`/`images`/`attachmentUrls` 等）在 DB 是 **string**（逗号分隔或 JSON），`FileUploader` 的 multiple 模式用 `string[]`。需用 computed 做转换。

**转换模式（每个文件的 `<script setup>` 里加）：**

```typescript
// 以 imageUrls 为例（string ↔ string[]）
const imageUrlsModel = computed<string[]>({
  get: () => {
    if (!form.imageUrls) return []
    try { return JSON.parse(form.imageUrls) } catch { return form.imageUrls.split(',') }
  },
  set: (val) => { form.imageUrls = val.length ? JSON.stringify(val) : '' }
})
```

模板里用 `<FileUploader v-model="imageUrlsModel" type="image" multiple module="..." />`。

- [ ] **步骤 1：goods/info/index.vue + detail/BasicTab.vue（imageUrls）**

两个文件各加 `imageUrlsModel` computed（绑定 form.imageUrls）。模板第 542 行 / detail 第 295 行 textarea 替换为：

```vue
            <FileUploader v-model="imageUrlsModel" type="image" multiple module="goods" />
```

- [ ] **步骤 2：park FacilityTab（images）+ RoomTab（images, additionalImages）**

- `FacilityTab.vue` 第 329 行 `form.images`：加 `imagesModel` computed，替换 textarea
- `RoomTab.vue` 第 639 行 `typeForm.images`、第 644 行 `typeForm.additionalImages`：各加 computed

- [ ] **步骤 3：supplier/contract（attachmentUrls）**

`resource/supplier/contract/index.vue` 第 501 行 `form.attachmentUrls`（textarea）：加 `attachmentUrlsModel` computed，替换。module=supplier。

- [ ] **步骤 4：service/session EvaluationTab（imageUrls）+ DemandTab（demandImages）**

- `service/session/detail/EvaluationTab.vue` 第 256 行 `form.imageUrls`：加 computed
- `service/session/detail/DemandTab.vue` 第 391 行 `form.demandImages`：加 computed

- [ ] **步骤 5：GUI 抽查多图上传/回显 + Commit**

验证：多图上传 → 保存 → 刷新 → 缩略图列表回显正确（JSON parse 正常）。

```bash
git add dayan-admin/src/views
git commit -m "feat(admin-ui): 8 处多图字段接入 FileUploader(string↔array 转换)"
```

---

## 任务 9：admin 前端批量替换视频/文件/VR 字段（~8 处）

- [ ] **步骤 1：goods/info videoUrl（index + detail）**

`goods/info/index.vue` 第 547 行、`detail/BasicTab.vue` 第 300 行：替换为 `<FileUploader v-model="form.videoUrl" type="video" module="goods" />`。

- [ ] **步骤 2：MediaVideoPane（videoUrl）**

`resource/park/detail/MediaVideoPane.vue` 第 241 行：`<FileUploader v-model="form.videoUrl" type="video" module="park" />`。展示位如有 el-image 改 formatFileUrl。

- [ ] **步骤 3：MediaFilePane（fileUrl）**

`resource/park/detail/MediaFilePane.vue` 第 217 行：`<FileUploader v-model="form.fileUrl" type="file" module="park" />`。

- [ ] **步骤 4：MediaVrPane（vrUrl）**

`resource/park/detail/MediaVrPane.vue` 第 233 行：`<FileUploader v-model="form.vrUrl" type="vr" module="park" />`。

- [ ] **步骤 5：finance/invoice（invoiceUrl）**

`finance/invoice/index.vue` 第 794 行 `issueForm.invoiceUrl`：`<FileUploader v-model="issueForm.invoiceUrl" type="file" module="finance" />`。

- [ ] **步骤 6：GUI 抽查视频/文件上传 + Commit**

```bash
git add dayan-admin/src/views
git commit -m "feat(admin-ui): 视频/文件/VR 字段接入 FileUploader(videoUrl/fileUrl/vrUrl/invoiceUrl)"
```

---

## 任务 10：channel 前端显示修复

channel 端无上传表单，只把只读展示位适配新 URL 形态（key → formatFileUrl 拼前缀）。

**文件：**
- 创建：`dayan-channel/src/utils/file.ts`
- 修改：`dayan-channel/src/layouts/default/index.vue`
- 修改：`dayan-channel/src/views/mall/index.vue`
- 修改：`dayan-channel/src/views/equity/index.vue`

- [ ] **步骤 1：创建 channel utils/file.ts**

```typescript
/**
 * channel 端 formatFileUrl：将 DB key 转为 channel-api preview URL。
 * http 开头的原样返回（兼容历史数据）。
 */
export function formatFileUrl(value: string | undefined | null): string {
  if (!value) return ''
  if (value.startsWith('http://') || value.startsWith('https://')) return value
  return `/channel-api/v1/files/preview/${value}`
}
```

- [ ] **步骤 2：修 layouts/default/index.vue（avatar）**

第 18 行 `avatarUrl` computed 改为用 formatFileUrl：
```typescript
import { formatFileUrl } from '@/utils/file'
const avatarUrl = computed(() => formatFileUrl(userStore.userInfo?.avatar))
```

- [ ] **步骤 3：修 mall/index.vue（coverImage）**

第 250 行 `<img :src="goods.coverImage">` 改为 `:src="formatFileUrl(goods.coverImage)"`，import formatFileUrl。

- [ ] **步骤 4：修 equity/index.vue（qrCodeUrl）**

第 287-289 行 `el-link :href` 用到 `currentEquity.qrCodeUrl`，用 formatFileUrl 包裹，import。

- [ ] **步骤 5：GUI 验证 channel 端显示 + Commit**

启动 channel 前端，登录后看：顶栏头像、商城商品图、我的权益二维码 能正常显示（用 admin 上传的图片测试）。

```bash
git add dayan-channel/src/utils/file.ts dayan-channel/src/layouts dayan-channel/src/views
git commit -m "feat(channel-ui): 只读展示位适配 key→preview URL(formatFileUrl)"
```

---

## 任务 11：全量 GUI 冒烟测试 + 兼容性验证

- [ ] **步骤 1：全 41 处逐页冒烟**

按侧边栏导航逐页面验证（参考 [[gui-testing-approach]]）：
- 每个替换处：上传 → 保存 → 刷新 → 图片正常显示
- 多图处：上传 2-3 张 → 保存 → 刷新 → 全部回显
- 视频/文件处：上传 → 保存 → 刷新 → 文件名/缩略图显示

- [ ] **步骤 2：兼容性验证（历史手填数据）**

若 DB 中有 http 开头的旧 URL 数据，验证这些页面刷新后仍能正常显示（formatFileUrl 原样透传 http 值）。

- [ ] **步骤 3：跨端共享验证**

admin 上传一张图（key=goods/day001/...），在 channel 端商城/详情页应能通过 `/channel-api/v1/files/preview/{key}` 正常显示同一张图。

- [ ] **步骤 4：修复发现的问题**

冒烟中发现的问题逐个修复（如有），单独 commit。

- [ ] **步骤 5：最终 Commit（如有修复）**

```bash
git add -A
git commit -m "fix(admin-ui): 文件上传全量接入冒烟测试修复"
```

---

## 自检

**规格覆盖度：**
- ✅ §3 三层架构 → 任务 1/2/3/4
- ✅ §4.1 StorageService 接口+Minio实现+配置 → 任务 2
- ✅ §4.2 FileController（admin upload+preview，channel preview）→ 任务 3/4
- ✅ §4.3 FileUploader 组件 → 任务 5
- ✅ §4.4 channel 显示修复 → 任务 10
- ✅ §5 数据流（上传+显示）→ 任务 6 验证
- ✅ §6 DB 存纯 key + formatFileUrl → 任务 5/10
- ✅ §8 41 处替换（18 单图 + 8 多图 + ~8 视频/文件/VR + 6 保留）→ 任务 6/7/8/9
- ✅ §9 错误处理（大小/类型/连接/404/兼容）→ 任务 2/3/5 内联
- ✅ §11 docker-compose → 任务 1
- ✅ §12 配置项 → 任务 3/4

无遗漏。

**占位符扫描：** 无 TODO/待定；所有代码步骤含完整代码块。多图 computed 转换模式在任务 8 步骤 1 给出完整模板，后续步骤复用同模式（已说明字段名差异，非"类似任务N"式省略）。

**类型一致性检查：**
- `StorageService` 方法签名（upload/download/delete/exists/contentType）在任务 2 定义，任务 3 FileAdminController 调用一致 ✅
- `FileUploadDTO` 字段（url/key/originalName/size）任务 2 定义，任务 5 前端 interface 一致 ✅
- `formatFileUrl(value)` 签名任务 5 定义，任务 10 channel 版本一致（仅前缀不同）✅
- FileUploader props（type/multiple/modelValue/module）任务 5 定义，任务 6-9 调用一致 ✅
- 前端 `request<T>` 用法任务 5 api/file.ts 遵循勘察到的模式 ✅

无误。
