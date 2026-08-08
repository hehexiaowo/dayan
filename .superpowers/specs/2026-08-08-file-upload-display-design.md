# 文件上传与显示 — 设计文档

- **日期**: 2026-08-08
- **状态**: 已批准（待 spec 自检 + 用户审查）
- **范围**: admin 全量接入（41 处手填 URL 输入框替换）+ channel 显示修复 + 基础存储能力
- **关联**: docs §7.6 / §5.6 / §20.1（原设计预签名直传，本轮 MVP 偏离为后端流式中转）

## 1. 背景与问题

dayan 项目所有图片/文件 URL 均靠手填完整地址存库，但手填的多为相对/内部地址，导致 `<el-image :src>` 渲染不出来——这是"图片显示不出来"的根因。

经全量盘点确认现状：
- `dayan-common-oss` 模块是 P0 空壳（仅 `package-info.java`，pom 无任何存储 SDK 依赖）
- 后端零上传接口（全仓 `MultipartFile` 命中数 0）
- 前端零 `el-upload`（element-plus 已安装但从未用过上传组件）
- DB 中 **64 列 / 27 张表** 存储 URL（剔除系统自动写入/外链/非文件字段后约 52 列需上传能力）
- admin 前端 **41 处 / 25 个 .vue** 手填 URL 输入框
- channel 前端 **0 个手填上传入口**（全是只读展示）
- `docker-compose.infra.yml` 无 MinIO 服务

## 2. 目标与非目标

### 目标
1. 建立 MinIO 存储能力（`dayan-common-oss` 实装）
2. admin 端 41 处手填 URL 输入框全部替换为 `<FileUploader>` 组件
3. 上传的图片/文件能正常显示（后端代理下载，同源零 CORS）
4. channel 端现有只读展示位（头像/logo/封面图/二维码）能正常渲染
5. DB 存端无关、环境无关的纯 key（相对路径），换环境数据不失效

### 非目标（本轮不做）
- OSS / COS 备选实现
- 预签名直传 + 回调确认（docs §5.6.3 原设计）
- CDN
- 上传审计/元数据表
- channel 端新建上传表单（本轮 channel 只修显示）
- 富文本编辑器内嵌上传
- agent / client 端（uview-ui uniapp）上传
- 系统自动写入字段的接入：`system_log_organ.request_url`、`system_operation_log.request_url`、`system_message.link_url`、`course_record_learn.certificate_url`、`equity_depot.qr_code_url`、`*_open_platform.api_base_url/callback_url`、`content_info.source_url`、`content_record_share.share_url`

## 3. 架构

### 3.1 三层架构

```
┌─────────────────────────────────────────────────┐
│  前端 (dayan-admin + dayan-channel)              │
│  <FileUploader> 组件(admin) + el-image/el-link   │
└────────────────────┬────────────────────────────┘
                     │ POST /files/upload (MultipartFile)
                     │ GET /files/preview/{key} (代理下载)
                     ▼
┌─────────────────────────────────────────────────┐
│  后端 (dayan-common-oss + 各端 FileController)    │
│  StorageService 接口                              │
│  └─ MinioStorageServiceImpl (MinIO SDK)           │
└────────────────────┬────────────────────────────┘
                     │ S3 协议
                     ▼
┌─────────────────────────────────────────────────┐
│  MinIO (docker-compose.infra.yml)                │
│  bucket: dayan-public (单 bucket, MVP)           │
└─────────────────────────────────────────────────┘
```

### 3.2 模块职责边界

| 模块 | 职责 |
|---|---|
| `dayan-common-oss` | 存储 SDK + `StorageService` 接口 + MinIO 实现 + 配置 + bucket 初始化 |
| `dayan-common-core` | `StorageException`（统一异常，复用现有统一返回包装） |
| admin / channel 端 | 各自的 `FileController`（薄层，调 `StorageService`） |

### 3.3 与 docs 原设计的偏离

docs §5.6.3 原设计是预签名 URL 前端直传（后端不承担流量）。本轮 MVP 改为**后端流式中转**，原因：

1. 开发环境 MinIO 跑 docker-compose，浏览器难以直连容器内网 endpoint（端口映射 + CORS + 双 endpoint 三件事都要对）
2. 后端中转一次请求原子完成"上传 + 入库 + 返回 URL"
3. 校验在后端不被绕过
4. 接口抽象预留：生产切直传零业务侵入（`StorageService` 接口不变，加 `presignUpload()` + `confirmUpload()` 回调，业务 Controller 不动）

**docs 不删改**，偏离仅在本设计文档记录。

## 4. 组件设计

### 4.1 后端 — dayan-common-oss

#### `StorageService` 接口

```java
public interface StorageService {
    /** 上传，返回生成的 key */
    String upload(String module, String channelCode, InputStream is, long size, String contentType, String originalName);

    /** 下载，返回 InputStream（调用方负责 close） */
    InputStream download(String key);

    /** 删除 */
    void delete(String key);

    /** 是否存在 */
    boolean exists(String key);

    /** 从 key 推断 contentType */
    String contentType(String key);
}
```

#### `MinioStorageServiceImpl`

- 注入 `StorageProperties` + MinIO `MinioClient`
- `upload()`：生成 key = `{module}/{channelCode}/{yyyy/MM/dd}/{uuid}.{ext}`，调 `client.putObject()`
- `download()`：调 `client.getObject()`
- `delete()`：调 `client.removeObject()`
- `exists()`：调 `client.statObject()`，异常处理（不存在返回 false）
- `contentType()`：用 `URLConnection.guessContentTypeFromName()` + 后缀兜底映射表

#### `StorageProperties`

```java
@ConfigurationProperties(prefix = "dayan.storage")
@Data
public class StorageProperties {
    private String endpoint = "http://localhost:9000";
    private String accessKey = "dayan";
    private String secretKey = "dayan12345";
    private String bucket = "dayan-public";
    private long maxSize = 10 * 1024 * 1024; // 10MB
}
```

密钥通过 yml 配置（开发）或 Nacos（生产），不进代码库。

#### `StorageAutoConfiguration`

- `@Bean MinioClient`：根据 `StorageProperties` 构建
- `@Bean StorageService`：注册 `MinioStorageServiceImpl`
- `@PostConstruct`（或 ApplicationRunner）：启动时检查 bucket 是否存在，不存在则创建（失败仅告警，不阻止启动）

配置加载方式：各端 application.yml 加 `dayan.storage.*` 配置项。

### 4.2 后端 — FileController（admin 端 + channel 端各一份）

> **放置决策**：每端是独立 Spring Boot 应用、独立 Sa-Token 域，故 FileController 各端一份。逻辑极薄（调 StorageService）。admin 端有 upload + preview；channel 端只有 preview（只读，因为本轮 channel 不加上传表单）。

#### admin 端 `FileController`

```
POST /admin-api/v1/files/upload
  入参: MultipartFile file, String module (可选,默认 "common")
  权限: 登录即可（不加 @SaCheckPermission，上传是基础能力，41 处表单通用）
  逻辑:
    1. 校验 contentType 白名单 + size ≤ maxSize
    2. 从 Sa-Token 获取当前 channelCode
    3. StorageService.upload(module, channelCode, is, size, contentType, originalName)
    4. 返回 FileUploadDTO { url, key, originalName, size }
  返回:
    {
      "code": 0,
      "data": {
        "url": "/admin-api/v1/files/preview/{key}",
        "key": "{key}",
        "originalName": "xxx.jpg",
        "size": 123456
      }
    }

GET /admin-api/v1/files/preview/{key}  (key 含路径分隔符, 用 ** 通配)
  权限: 无（或登录态，使能 el-image 直接加载）
  逻辑:
    1. StorageService.download(key) → InputStream
    2. setHeader Content-Type = StorageService.contentType(key)
    3. setHeader Cache-Control: max-age=86400 (静态资源缓存)
    4. 流式写 ResponseBody
```

> **路径变量注意**：key 形如 `goods/day001/2026/08/08/abc.jpg` 含 `/`，Spring MVC `@PathVariable String key` 会吞斜杠。需用 `@PathVariable("key") String key` + `/**/*.png` 风格 ant pattern，或 `@RequestMapping("/preview/**")` + `HttpServletRequest` 提取 pathAfter。采用后者（参考 [[spring-mvc-path-variable-eats-literal]] 经验）。

#### channel 端 `FileController`

```
GET /channel-api/v1/files/preview/{key}
  逻辑同 admin preview，复用同一 StorageService
```

仅此一个端点（只读）。

#### `FileUploadDTO`

```java
@Data
public class FileUploadDTO {
    private String url;         // /admin-api/v1/files/preview/{key}
    private String key;         // goods/day001/2026/08/08/abc.jpg
    private String originalName;
    private long size;
}
```

### 4.3 前端 — `<FileUploader>` 组件

#### Props

| Prop | 类型 | 默认 | 说明 |
|---|---|---|---|
| `type` | `'image'\|'video'\|'file'\|'vr'\|'any'` | `'any'` | 决定 accept + 图标 + UI 形态 |
| `multiple` | `boolean` | `false` | false=单文件返回 string, true=多文件返回 string[] |
| `modelValue` | `string \| string[]` | — | v-model，存**纯 key**（见 §6） |
| `accept` | `string` | 按 type 推断 | 覆盖默认 accept |
| `maxSize` | `number` | `10` | MB |
| `limit` | `number` | `9` | multiple 时最大数量 |
| `disabled` | `boolean` | `false` | 只读模式 |

#### 默认 accept 映射

| type | accept |
|---|---|
| image | `image/jpeg,image/png,image/gif,image/webp` |
| video | `video/mp4,video/webm` |
| file | `.pdf,.doc,.docx,.xls,.xlsx,.ppt,.pptx,.txt` |
| vr | 不限（VR 是外链，但也支持上传全景图包） |
| any | 不设 |

#### UI 形态

- **image (single)**：方块虚线框（80×80）+ 缩略图预览，hover 显示替换/删除
- **image (multiple)**：缩略图网格 + 末尾虚线框（点击添加）
- **video**：方块 + 视频图标 + 文件名
- **file**：按钮式（"点击上传"）+ 文件列表（文件名 + 大小 + 删除）
- **vr**：按钮式 + 已传文件名展示

#### 内部逻辑

- `:http-request` 自定义上传（不走 el-upload 默认 action）：调 `uploadFile(formData)` API
- 上传中显示进度（el-upload 内置 progress）
- 上传成功：`emit('update:modelValue', url)`
- 上传失败：`ElMessage.error` + 不改变 modelValue
- 删除：`emit('update:modelValue', multiple ? arr.filter(...) : '')`
- before-upload 校验 type/size，前端预校验减少无效请求

#### 前端 API（`dayan-admin/src/api/file.ts`）

```typescript
export function uploadFile(file: File, module?: string) {
  const formData = new FormData()
  formData.append('file', file)
  if (module) formData.append('module', module)
  return request<FileUploadDTO>({
    url: '/admin-api/v1/files/upload',
    method: 'post',
    data: formData,
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}
```

### 4.4 前端 — channel 端显示修复

channel 端现有只读展示位需要适配新的 URL 形态（§6 DB 值为 key，前端需拼接 preview 前缀）。两种处理：

**方案 A（MVP 采用）**：channel 端写一个 `formatFileUrl(key)` 工具函数，在展示处拼接 `/channel-api/v1/files/preview/` 前缀。改动点：
- `views/equity/index.vue`（qrCodeUrl）
- `views/mall/index.vue`（coverImage）
- `layouts/default/index.vue`（avatar）
- 其余只读位（apiBaseUrl 等非文件 URL 不动）

**方案 B**：DB 存完整 url（含前缀），channel 直接渲染。但这样违背"DB 存环境无关值"原则，放弃。

## 5. 数据流

### 5.1 上传流

```
1. 用户在 <FileUploader> 选文件
2. el-upload :http-request → POST /admin-api/v1/files/upload
3. 后端 FileController:
   a. 校验 contentType (白名单) + size (≤10MB)
   b. 从 Sa-Token 取 channelCode
   c. 生成 key = {module}/{channelCode}/{yyyy/MM/dd}/{uuid}.{ext}
   d. StorageService.upload(key, inputStream, size, contentType)
   e. 返回 FileUploadDTO {url, key, originalName, size}
4. FileUploader 取 DTO.key 存入 modelValue，emit('update:modelValue', key)  // 存 key, 见 §6
   （DTO.url 仅用于上传后即时预览，不持久化）
5. 提交业务表单 → key 存入 DB xxx_url 列
```

### 5.2 显示流（后端代理下载）

```
1. <el-image :src="formatFileUrl(row.imageUrl)">
   formatFileUrl(key) = "/admin-api/v1/files/preview/" + key
2. 浏览器请求 → Vite proxy (开发) / Gateway (生产) → admin 服务
3. FileController.preview(key):
   a. StorageService.download(key) → InputStream
   b. setHeader Content-Type = contentType(key)
   c. setHeader Cache-Control: max-age=86400
   d. 流式写 ResponseBody
4. 浏览器渲染
```

## 6. DB 值的形态（关键决策）

**DB 的 `xxx_url` 列存什么？** 这是核心决策，影响跨端共享与换环境。

| 方案 | 存什么 | 优点 | 缺点 |
|---|---|---|---|
| **A. 纯 key** ✅ | `goods/day001/2026/08/08/abc.jpg` | 端无关、环境无关；admin/channel 各自拼前缀；换域名/换环境数据不失效 | 前端展示需 formatFileUrl 拼接 |
| B. 完整 url | `http://localhost:8080/admin-api/v1/files/preview/...` | 前端零拼接 | 换环境数据失效；多端前缀不同导致 admin 存的 channel 渲染错位 |

**采用方案 A（纯 key）**。

- DB 存纯 key（如 `goods/day001/2026/08/08/abc.jpg`）
- admin 前端展示：`formatFileUrl(key) = "/admin-api/v1/files/preview/" + key`
- channel 前端展示：`formatFileUrl(key) = "/channel-api/v1/files/preview/" + key`
- 同一份数据，两端各自拼接自己的 preview 前缀

**兼容历史手填数据**：已存在的手填完整 URL（如 `http://...`）保持原样显示——`formatFileUrl` 判断：若值以 `http` 开头则原样返回，否则拼接前缀。这样不会破坏现有数据。

## 7. 文件命名与存储约定

- 单 bucket `dayan-public`（MVP 简化；敏感文件靠 UUID 文件名提供基本安全）
- 命名：`{module}/{channelCode}/{yyyy/MM/dd}/{uuid}.{ext}`（遵循 docs §5.6.2）
  - module 例：goods / scene / park / course / content / equity / organ / supplier / channel / common
  - channelCode：从 Sa-Token 获取当前登录渠道编码
- 不保留原始文件名（防中文/特殊字符/路径穿越），originalName 仅在 FileUploadDTO 返回给前端

## 8. 41 处替换清单

按 type 分类（完整清单见 brainstorming 盘点结果）：

### 单图 (type=image, single) — 约 18 处
| 字段 | 页面 |
|---|---|
| `logoUrl` | channel/info/index.vue, channel/info/detail/BasicTab.vue, resource/supplier/index.vue |
| `coverImage` | goods/info/index.vue, goods/info/detail/BasicTab.vue, resource/scene/index.vue, resource/scene/detail/BasicTab.vue, resource/course/index.vue, resource/content/index.vue, equity/template/index.vue |
| `coverImage` (park 子表) | resource/park/detail/FoodTab.vue, FacilityTab.vue, RoomTab.vue, ServiceItemPane.vue |
| `avatar` | channel/info/detail/AccountTab.vue, channel/agent/index.vue, channel/client/index.vue, service/butler/index.vue, service/butler/detail/BasicTab.vue |
| `adviserImage` | resource/park/detail/AdviserTab.vue |
| `designImage` | resource/park/detail/RoomTab.vue |
| `coverUrl` | resource/park/detail/MediaVideoPane.vue |
| `thumbnailUrl` | resource/park/detail/MediaVrPane.vue |
| `cardDesignUrl` | equity/template/index.vue |

### 多图 (type=image, multiple, JSON 数组) — 约 8 处
| 字段 | 页面 |
|---|---|
| `imageUrls` | goods/info/index.vue, goods/info/detail/BasicTab.vue, service/session/detail/EvaluationTab.vue |
| `images` | resource/park/detail/FacilityTab.vue, RoomTab.vue |
| `attachmentUrls` | resource/supplier/contract/index.vue |
| `demandImages` | service/session/detail/DemandTab.vue |
| `additionalImages` | resource/park/detail/RoomTab.vue |

### 单视频 (type=video) — 约 4 处
| 字段 | 页面 |
|---|---|
| `videoUrl` | goods/info/index.vue, goods/info/detail/BasicTab.vue, resource/park/detail/MediaVideoPane.vue |
| `vrUrl`（VR 全景视频） | resource/park/detail/MediaVrPane.vue |

### 单文件 (type=file) — 约 3 处
| 字段 | 页面 |
|---|---|
| `fileUrl` | resource/park/detail/MediaFilePane.vue |
| `invoiceUrl` | finance/invoice/index.vue |
| `vrUrl`（VR 链接，按 file 处理） | resource/park/detail/MediaVrPane.vue（若与 video 项冲突，vr 单独归类） |

### VR (type=vr) — 约 2 处
| 字段 | 页面 |
|---|---|
| `vrUrl` | resource/park/detail/MediaVrPane.vue |
| `imageUrl` (media-image) | resource/park/detail/MediaImagePane.vue |

### 不接入（保留 el-input 手填） — 约 6 处
| 字段 | 原因 |
|---|---|
| `apiBaseUrl`、`callbackUrl` | API 配置，非文件 |
| `sourceUrl` | 外链转载，非上传 |
| `shareUrl` | 分享外链，非上传 |

> **注**：MediaVrPane 的 `vrUrl` 在盘点里是必填项，业务上 VR 既可能是上传的全景包也可能是外链。MVP 按 file 类型处理（上传），若用户填外链则 formatFileUrl 的 http 判断会兼容。

## 9. 错误处理

| 场景 | 处理 |
|---|---|
| 文件超 10MB | 后端抛 `StorageException`，统一返回包装 → 前端 ElMessage.error |
| 类型不在白名单 | 后端拒绝，前端提示允许的类型 |
| MinIO 连接失败（upload 时） | 后端抛异常，统一返回 500，前端 ElMessage.error |
| MinIO 连接失败（启动时） | `StorageAutoConfiguration` 建 bucket 失败 → 日志告警但不阻止启动；上传接口运行时报 503 |
| preview 的 key 不存在 | 返回 404 |
| 上传中断/网络错误 | el-upload `onError` → ElMessage.error + 可重试 |
| 历史手填数据（http 开头） | formatFileUrl 原样返回，不破坏现有数据 |

## 10. 测试策略

| 层 | 测试方式 |
|---|---|
| StorageService | 集成测试：本地 docker MinIO，upload→download→delete 往返验证 |
| FileController | MockMvc 上传测试（mock StorageService），验证校验逻辑 |
| `<FileUploader>` | 手动 GUI 测试（逐页面验证上传 + 显示） |
| 全量替换验证 | 41 处逐页冒烟：上传→保存→刷新→图片显示 |
| channel 显示 | 验证 admin 上传的图片在 channel 端 formatFileUrl 后能正常渲染 |
| 兼容性 | 验证历史手填 http URL 仍能显示 |

## 11. docker-compose.infra.yml 新增

```yaml
dayan-minio:
  image: minio/minio:latest
  container_name: dayan-minio
  ports:
    - "9000:9000"   # API
    - "9001:9001"   # Console
  environment:
    MINIO_ROOT_USER: dayan
    MINIO_ROOT_PASSWORD: dayan12345
  volumes:
    - minio-data:/data
  command: server /data --console-address ":9001"
```

并在 volumes 段加 `minio-data:`。

## 12. 配置项

各端 `application.yml`（或 nacos 配置）新增：

```yaml
dayan:
  storage:
    endpoint: http://localhost:9000
    access-key: dayan
    secret-key: dayan12345
    bucket: dayan-public
    max-size: 10485760  # 10MB
```

spring.mvc.multipart 配置（确保能接收大文件）：
```yaml
spring:
  servlet:
    multipart:
      max-file-size: 10MB
      max-request-size: 10MB
```

## 13. 实现顺序建议

1. docker-compose 加 MinIO + 启动验证（console 可访问）
2. `dayan-common-oss`：pom 加 SDK + StorageService 接口 + Minio 实现 + 配置类
3. admin 端 application.yml 配置 + FileController（upload + preview）
4. 单接口验证（Postman/curl 上传 + 浏览器访问 preview）
5. 前端 `<FileUploader>` 组件 + `api/file.ts`
6. 单页接入验证（MediaImagePane 图片上传 + 显示）
7. admin 41 处批量替换（按 type 分批）
8. channel 端 formatFileUrl 工具 + 展示位适配
9. 全量 GUI 逐页冒烟测试
