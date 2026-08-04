# P0 脚手架 & 基础设施 - 设计规格

> **版本**：v1.0
> **编制日期**：2026-08-04
> **适用范围**：大雁养老服务权益平台 P0 阶段（脚手架 & 基础设施，1 周工期）
> **依据**：`docs/08项目计划书.md` §2.1 P0、`docs/03系统架构设计文档.md` §3-§5、`docs/06项目开发规范.md` §1.1-§1.4、`docs/02数据库设计文档_v4.1.md`
> **设计文档冲突说明**：本规格在以下两点偏离计划书原文，业经用户确认：
> - CI/CD：GitLab CI + Harbor → **GitHub Actions + GHCR**（用户实际使用 GitHub，本地有 Docker）
> - 工作目录：后端 Maven 多模块统一聚合在 `dayan-server/` 下（与 `docs/06` 规范命名一致），前端 4 个工程（dayan-web-admin / dayan-web-channel / dayan-miniprogram-agent / dayan-miniprogram-client）平级置于仓库根

---

## 一、范围决策（已与用户确认）

| 决策项 | 选定方案 | 备注 |
|--------|---------|------|
| 启动范围 | **完整 P0**（A） | 17 后端模块 + 4 前端工程 + 127 表 DDL + CI/CD + 四端登录 + 状态机引擎 |
| CI/CD | **GitHub Actions 流水线骨架**（B） | `.github/workflows/` 全套，GHCR 推送，本地可 `docker compose up` 验证 |
| DDL | **从 v4.1 文档完整抽取 127 张表**（A） | 按域分 17 个 SQL 文件，存放 `db/migration/` |
| 架构形态 | **6 独立启动模块 + 共享业务依赖**（C） | 6 端 + gateway + job 共 8 进程，docker compose 一键拉起 |
| 四端登录 | **完整四端登录闭环**（A） | Sa-Token 多端隔离 + 选渠道 + 登录日志 + BCrypt |
| 状态机 | **独立引擎实现 + 4 个状态机配置数据初始化**（A） | 引擎存 DB + Redis 缓存 |

---

## 二、技术栈锁定（P0 冻结）

### 2.1 后端

| 组件 | 版本 | 用途 |
|------|------|------|
| JDK | 21 (LTS) | 运行时 |
| Spring Boot | 3.2.x | 应用主框架 |
| Spring Cloud | 2023.0.x | 微服务体系 |
| Spring Cloud Alibaba | 2023.0.x | Nacos / Sentinel |
| MyBatis-Plus | 3.5.x | ORM + 多租户 + 分页 + 乐观锁 |
| Sa-Token | 1.37.x | 多端认证授权 |
| MySQL Connector/J | 8.x | 数据库驱动 |
| Redis (Lettuce) | 内置 | 缓存 / 会话 / 分布式锁 |
| Hutool | 5.8.x | 工具集 |
| Knife4j | 4.x | 接口文档 |
| MapStruct | 1.5.x | DTO/Entity 转换 |
| Lombok | 最新 | 简化样板 |
| BouncyCastle | 1.77 | AES-256-GCM 加密 |
| Spring Security Crypto | 内置 | BCrypt 密码哈希 |

### 2.2 前端

| 端 | 技术栈 |
|----|--------|
| dayan-web-admin | Vue 3.4 + TypeScript + Vite + Element Plus 2.7 + Pinia + Vue Router 4 + Axios |
| dayan-web-channel | 同 Admin 模板 |
| dayan-miniprogram-agent | uni-app + uView UI 2.x + Pinia (uni 版) |
| dayan-miniprogram-client | uni-app + uView UI 2.x + Pinia (uni 版) |

### 2.3 基础设施

| 组件 | 版本 | 用途 |
|------|------|------|
| MySQL | 8.0.x | 主数据库 |
| Redis | 7.2.x | 缓存 |
| Nacos | 2.3.x | 配置中心 + 服务发现 |
| Docker | 24.x | 容器化 |
| Docker Compose | v2 | 本地编排 |
| GitHub Actions | - | CI/CD |

---

## 三、项目目录结构

仓库根为 `F:/code/dayan`。后端 Maven 多模块统一聚合在 `dayan-server/` 下；前端 4 个工程、数据库脚本、Dockerfile、CI 配置平级置于仓库根（前后端构建体系分离，互不通过 Maven 聚合）。

```
dayan/
├── docs/                              # 设计文档（已存在，含本规格）
├── .superpowers/                      # AI 辅助开发规格与计划
├── dayan-server/                      # 后端聚合根（Maven 多模块）
│   ├── pom.xml                        # 后端父 POM（统一版本管理）
│   ├── dayan-common/                  # 公共模块（聚合）
│   │   ├── dayan-common-bom/          # 内部模块版本 BOM
│   │   ├── dayan-common-core/         # R<T>/异常/常量/工具/雪花 ID/编码生成/AES
│   │   ├── dayan-common-redis/        # Redis 封装
│   │   ├── dayan-common-mybatis/      # MyBatis-Plus 配置与插件（分页/自动填充/租户/乐观锁）
│   │   ├── dayan-common-security/     # Sa-Token 多端鉴权
│   │   ├── dayan-common-log/          # 日志切面 + trace_id + 脱敏
│   │   ├── dayan-common-mq/           # 消息队列（预留，P0 空壳）
│   │   ├── dayan-common-oss/          # 文件存储（预留，P0 空壳）
│   │   ├── dayan-common-swagger/      # Knife4j 配置
│   │   ├── dayan-common-sms/          # 短信（预留，P0 空壳）
│   │   ├── dayan-common-pay/          # 支付（预留，P0 空壳）
│   │   └── dayan-common-lbs/          # 地理位置（预留，P0 空壳）
│   ├── dayan-gateway/                 # Spring Cloud Gateway（端口 8000）
│   ├── dayan-modules/                 # 17 个业务模块
│   │   ├── dayan-module-system/       # 系统域（18 表）
│   │   ├── dayan-module-organ/        # 核心域（9 表）
│   │   ├── dayan-module-butler/       # 养老管家域（8 表）
│   │   ├── dayan-module-supplier/     # 供应商域（10 表）
│   │   ├── dayan-module-park/         # 养老机构域（15 表）
│   │   ├── dayan-module-scene/        # 场景域（5 表）
│   │   ├── dayan-module-channel/      # 渠道域（11 表）
│   │   ├── dayan-module-agent/        # 代理人域（6 表）
│   │   ├── dayan-module-client/       # 客户域（7 表）
│   │   ├── dayan-module-equity/       # 权益域（6 表）
│   │   ├── dayan-module-service/      # 服务域（7 表）
│   │   ├── dayan-module-goods/        # 商品域（5 表）
│   │   ├── dayan-module-content/      # 内容域（5 表）
│   │   ├── dayan-module-course/       # 课程域（3 表）
│   │   ├── dayan-module-order/        # 订单域（4 表）
│   │   ├── dayan-module-finance/      # 结算域（7 表）
│   │   └── dayan-module-distributor/  # 分销商域（1 表）
│   ├── dayan-starters/                # 启动模块
│   │   ├── dayan-admin/               # Admin 端（端口 8080，/admin-api/）
│   │   ├── dayan-channel/             # Channel 端（端口 8081，/channel-api/）
│   │   ├── dayan-agent/               # Agent 端（端口 8082，/agent-api/）
│   │   ├── dayan-client/              # Client 端（端口 8083，/client-api/）
│   │   ├── dayan-supplier/            # Supplier 端（端口 8084，/supplier-api/，后端预留最小 1 副本）
│   │   └── dayan-distributor/         # Distributor 端（端口 8085，/distributor-api/，后端预留最小 1 副本）
│   └── dayan-job/                     # 定时任务模块（独立部署，不对外 HTTP）
├── dayan-web-admin/                   # Admin 前端（Vue3 + TS + Element Plus）
├── dayan-web-channel/                 # Channel 前端（同 Admin 模板）
├── dayan-miniprogram-agent/           # Agent 小程序/H5（uni-app）
├── dayan-miniprogram-client/          # Client 小程序/H5（uni-app）
├── db/
│   └── migration/
│       ├── 01_system.sql              # 系统域 18 表 DDL
│       ├── 02_organ.sql               # 核心域 9 表
│       ├── 03_butler.sql              # 管家域 8 表
│       ├── 04_supplier.sql            # 供应商域 10 表
│       ├── 05_park.sql                # 机构域 15 表
│       ├── 06_scene.sql               # 场景域 5 表
│       ├── 07_channel.sql             # 渠道域 11 表
│       ├── 08_agent.sql               # 代理人域 6 表
│       ├── 09_client.sql              # 客户域 7 表
│       ├── 10_equity.sql              # 权益域 6 表
│       ├── 11_service.sql             # 服务域 7 表
│       ├── 12_goods.sql               # 商品域 5 表
│       ├── 13_content.sql             # 内容域 5 表
│       ├── 14_course.sql              # 课程域 3 表
│       ├── 15_order.sql               # 订单域 4 表
│       ├── 16_finance.sql             # 结算域 7 表
│       ├── 17_distributor.sql         # 分销商域 1 表
│       └── seed/
│           ├── dict_seed.sql          # 字典数据
│           ├── menu_seed.sql          # 四端菜单树
│           ├── state_machine_seed.sql # 4 个状态机配置
│           └── admin_seed.sql         # 超级管理员 admin/admin123 (BCrypt)
├── docker/
│   ├── admin/Dockerfile
│   ├── channel/Dockerfile
│   ├── agent/Dockerfile
│   ├── client/Dockerfile
│   ├── supplier/Dockerfile
│   ├── distributor/Dockerfile
│   ├── gateway/Dockerfile
│   └── job/Dockerfile
├── .github/
│   └── workflows/
│       ├── ci.yml                     # lint → test → build
│       └── docker.yml                 # build → push GHCR
├── docker-compose.yml                 # 一键拉起 MySQL + Redis + Nacos + 八端
├── docker-compose.infra.yml           # 仅基础设施（MySQL/Redis/Nacos）
├── pom.xml                            # 父 POM（统一版本管理）
├── .editorconfig
├── .gitignore                         # 更新
└── README.md                          # 启动说明
```

> **模块内部分层**遵循 `docs/06项目开发规范.md` §1.1：`controller/{admin,channel,agent,client,supplier,distributor,open}/`、`service/impl`、`mapper`、`entity`、`dto`、`vo`、`config`、`converter`、`enums`、`statemachine`。

---

## 四、后端骨架设计

### 4.1 父 POM（`pom.xml`）

- `<packaging>pom</packaging>`，`<modules>` 聚合所有子模块
- `dependencyManagement` 锁定全部版本（Spring Boot BOM / Spring Cloud BOM / Spring Cloud Alibaba BOM / MyBatis-Plus / Sa-Token / Hutool / Knife4j / MapStruct）
- JDK 21，编码 UTF-8
- 公共插件：`maven-compiler-plugin`（JDK 21）、`maven-surefire-plugin`、`jacoco-maven-plugin`（覆盖率统计，门禁阈值留待 P1）

### 4.2 dayan-common-core

| 类 | 职责 |
|----|------|
| `R<T>` | 统一响应封装 `code/message/data/timestamp/traceId` |
| `PageResult<T>` | 分页响应 |
| `ErrorCode` | 错误码枚举（10000/10100/10101/10200/10201/10300/10400/10500） |
| `BusinessException` | 业务异常基类 |
| `ParamException` / `UnauthorizedException` / `TokenExpiredException` / `ForbiddenException` / `AccountLockedException` / `NotFoundException` | 派生异常（错误码按 §1.4） |
| `GlobalExceptionHandler` | `@RestControllerAdvice` 兜底：业务异常/参数异常/Sa-Token 异常/系统异常 |
| `BusinessCode` | 业务编码常量前缀（EQ/SP/PK/CL/AG/DS…） |
| `SnowflakeId` | 雪花算法 ID 生成（分片候选表用） |
| `CodeGenerator` | 业务编码生成器（`PK`+5 位等，Redis INCR 并发安全） |
| `AesGcmUtil` | AES-256-GCM 加解密（BouncyCastle） |
| `DateUtil` 等 | 复用 Hutool，封装项目专用方法 |

### 4.3 dayan-common-mybatis

- `MybatisPlusConfig`：注册 `PaginationInnerInterceptor`、`OptimisticLockerInnerInterceptor`、`TenantLineInnerInterceptor`
- `TenantHandlerImpl`：实现 `TenantLineHandler`，`getTenantId()` 从 Sa-Token 上下文取 `channel_code`；维护**忽略表清单**（系统域/organ 域等平台共享表不入租户拦截）
- `MetaObjectHandlerImpl`：自动填充 `created_at` / `updated_at` / `creator` / `updater`（INSERT 填充全部，UPDATE 填充后两项）
- 字段命名：数据库下划线 ↔ Java 驼峰（`map-underscore-to-camel-case: true`）
- 逻辑删除：`@TableLogic`，`deleted` 字段 `1=已删除/0=未删除`，配合 `deleted_at` 记录删除时间

### 4.4 dayan-common-security

- `SaTokenConfig`：多端配置，四命名空间
  - `StpUtilAdmin`（admin）、`StpUtilChannel`、`StpUtilAgent`、`StpUtilClient`
  - 各自独立 `tokenName`（如 `Admin-Token` / `Agent-Token`）+ 独立 Redis key 前缀
- `SaTokenContextFilter`：解析 Token 后向 ThreadLocal 注入 `channel_code` / `account_id` / `account_type`
- 登录日志切面：登录成功/失败异步落库 `system_login_log`
- 密码：`BCryptPasswordEncoder`，强度 10

### 4.5 dayan-common-redis

- `RedisConfig`：Lettuce 连接池、`RedisTemplate<String, Object>`（Jackson 序列化）、`StringRedisTemplate`
- Key 前缀规范：`dayan:{module}:{biz}:{id}`（如 `dayan:sm:rule:EQUITY_SM:0:activate`）
- 分布式锁：基于 Redisson 或 Redis SET NX（P0 用 SET NX 简化）

### 4.6 dayan-common-log

- `@OperationLog` 注解 + AOP 切面，记录 6 类账号操作到 `system_operation_log`
- `@Async` 异步落库，不阻塞主流程
- `TraceIdFilter`：每个请求生成 `traceId`（UUID），写入 MDC + 响应头
- 脱敏规则：手机号/身份证/银行卡（正则脱敏）

### 4.7 公共模块预留（mq/oss/swagger/sms/pay/lbs）

P0 仅创建 Maven 模块 + 空接口/配置类骨架，不接真实第三方服务。提供：
- `dayan-common-swagger`：Knife4j 自动配置（各启动模块自动启用）
- 其余（mq/oss/sms/pay/lbs）：仅 `pom.xml` + 占位 `package-info.java`，待对应业务域阶段填充

### 4.8 业务模块骨架（17 个）

每个 `dayan-module-xxx` 包含：
- `pom.xml`（依赖 `dayan-common-core` + `dayan-common-mybatis` + `dayan-common-security`）
- 按规范 §1.1 分层目录：`controller/{端}/`、`service/impl`、`mapper`、`entity`、`dto`、`vo`、`enums`
- Entity 完整覆盖该域所有表（127 表全量映射，P0 即建好）
- Mapper 继承 `BaseMapper<T>`，按需提供 `selectByCode` 等 default 方法
- Service 提供基础 CRUD 接口骨架（P0 不强制写全部业务逻辑，但 entity/mapper 完整可用）

> **P0 业务模块完成度定义**：Entity/Mapper/Service-接口完整（127 表全量），Controller 仅系统域 + organ 域 + channel/agent/client 登录相关接口可用；其余域 Controller 在 P1-P7 增量补全。

### 4.9 dayan-gateway

- Spring Cloud Gateway，端口 8000
- 路由：`/admin-api/**` → `dayan-admin`、`/channel-api/**` → `dayan-channel`…（lb://服务名）
- 全局过滤器：Sa-Token Token 解析（不强制鉴权，转交下游）+ traceId 透传 + 限流（Sentinel，P0 仅配置骨架）

### 4.10 启动模块（dayan-starters/）

| 启动模块 | 端口 | context-path | 业务依赖（dayan-module-） |
|---------|------|--------------|-------------------------|
| dayan-admin | 8080 | /admin-api | system + organ + channel + agent + client + equity + service + order + finance + park + supplier + butler + goods + scene + content + course + distributor |
| dayan-channel | 8081 | /channel-api | system + organ + channel + agent + client + park + goods + scene + content |
| dayan-agent | 8082 | /agent-api | system + organ + agent + client + equity + service + goods + scene + content + course |
| dayan-client | 8083 | /client-api | system + organ + agent + client + equity + service + goods + scene + content + course |
| dayan-supplier | 8084 | /supplier-api | system + organ + supplier + park + goods + scene |
| dayan-distributor | 8085 | /distributor-api | system + organ + distributor + channel + park + goods |

每个启动模块：
- 独立 `@SpringBootApplication`
- 独立 `application.yml` + `application-{dev,sit,uat,prod}.yml`
- 独立 Dockerfile + Docker 镜像
- `bootstrap.yml` 注册到 Nacos + 拉取配置

### 4.11 dayan-job

- Spring Boot 应用，不对外 HTTP
- 定时任务骨架：权益过期扫描 / 订单超时取消 / 自动对账（P0 仅注册占位 `@Scheduled` 方法，真正逻辑在 P4/P7 补）
- K8s CronJob 部署（P0 用 docker compose 跑）

---

## 五、数据库初始化设计

### 5.1 DDL 生成策略

- **数据源**：`docs/02数据库设计文档_v4.1.md`（唯一真相源）
- **输出**：`db/migration/` 下 17 个 SQL 文件，按域编号（01-17），MySQL 8.0 方言
- **字符集**：`utf8mb4` / `utf8mb4_unicode_ci`
- **引擎**：`InnoDB`
- **每表必须项**：
  - 主键（`id BIGINT` 自增 or 雪花，按 v4.1 §2.0.1 分片候选表用雪花 ID）
  - `entity_code` 业务编码（UK 唯一索引，按域前缀）
  - `channel_code` 字段（分片候选表必填，含索引）
  - 公共字段：`created_at` / `updated_at` / `creator` / `updater` / `deleted` (TINYINT 1) / `deleted_at`
  - 业务索引（按 v4.1 §9 索引优化策略）
  - 表注释 + 字段注释（COMMENT）
- **执行顺序**：`docker-compose.yml` 中 MySQL 容器 `volumes` 挂载 `db/migration` 到 `/docker-entrypoint-initdb.d`，按文件名字典序自动执行（01 → 17 → seed）

### 5.2 种子数据（`db/migration/seed/`）

| 文件 | 内容 |
|------|------|
| `dict_seed.sql` | `system_dict_common` / `system_dict_business`（17 域）/ `system_dict_region`（省市区三级）/ `system_dict_iplocation`（占位） |
| `menu_seed.sql` | `system_menu` 四端菜单树（Admin/Channel/Agent/Client） |
| `state_machine_seed.sql` | `system_state_machine` 4 个状态机：EQUITY_SM（8 态）/ SERVICE_SESSION_SM（7 态+子状态）/ ORDER_SM（8 态）/ PARK_SM（4 态） |
| `admin_seed.sql` | `organ_account` 超级管理员 `admin` / 密码 `admin123`（BCrypt 哈希）+ `organ_employee` + `organ_info` 大雁养老公司 |

### 5.3 验证

- DDL 执行后 `information_schema.tables` 统计 = 127
- 种子数据计数：字典/菜单/状态机/管理员各 ≥1
- 全部 UK 索引就位

---

## 六、四端登录闭环设计

### 6.1 账号表与登录方式

| 端 | 账号表 | 登录方式 | Token 命名空间 |
|----|--------|---------|---------------|
| Admin | `organ_account` | username / mobile / email + password | `StpUtilAdmin` |
| Channel | `channel_account` | username / mobile / email + password | `StpUtilChannel` |
| Agent | `agent_account` | open_id / mobile + password（至少一项）；支持选渠道 | `StpUtilAgent` |
| Client | `client_account` | open_id / mobile + password（至少一项）；支持选渠道 | `StpUtilClient` |

### 6.2 登录接口（P0 实现）

| 端 | 路径 | 方法 |
|----|------|------|
| Admin | `POST /admin-api/auth/login` | username + password |
| Admin | `POST /admin-api/auth/logout` | - |
| Admin | `GET /admin-api/auth/info` | 当前登录人 |
| Channel | `POST /channel-api/auth/login` | username + password |
| Channel | `POST /channel-api/auth/logout` | - |
| Channel | `GET /channel-api/auth/info` | - |
| Agent | `GET /agent-api/auth/channels` | 按 mobile/open_id 检索关联渠道 |
| Agent | `POST /agent-api/auth/login` | 选定 channel_code + mobile/open_id + password |
| Agent | `POST /agent-api/auth/logout` | - |
| Agent | `GET /agent-api/auth/info` | - |
| Client | `GET /client-api/auth/channels` | 同上 |
| Client | `POST /client-api/auth/login` | 同上 |
| Client | `POST /client-api/auth/logout` | - |
| Client | `GET /client-api/auth/info` | - |

### 6.3 选渠道特性（Agent/Client）

1. 用户在登录页输入手机号或微信授权获得 `open_id`
2. 调用 `/auth/channels?mobile=xxx`（或 `open_id=xxx`）→ 后端按自然键检索 `agent_account`（或 `client_account`）所有渠道记录，返回渠道列表
3. 用户选定 `channel_code`
4. 调用 `/auth/login` 携带 `channel_code` + 凭证 → 登录成功后 Token 绑定该 `channel_code`，写入 Sa-Token Session
5. 后续请求租户拦截器从 Session 取 `channel_code` 自动隔离

### 6.4 安全策略

- 密码 BCrypt（strength=10）
- 登录失败 5 次锁定 30 分钟（Redis 计数）
- 登录日志异步落 `system_login_log`（IP/UA/设备指纹/result）
- Token 有效期：Admin/Channel 2h、Agent/Client 7d
- P0 阶段权限校验用"超级管理员通放"占位（`@SaCheckPermission` 注解先不强制，P1 RBAC 就绪后启用）

### 6.5 跨域登录（微信）

P0 微信登录：
- `jscode2session` 接口对接预留（`dayan-common-security` 提供 `WechatLoginService` 接口）
- P0 用 mock 模式：`open_id = mock_{mobile}`，等 P2 阶段接入真实微信小程序

---

## 七、状态机引擎设计

### 7.1 接口定义（dayan-common-core）

```java
public interface StateMachineEngine {
    /** 校验状态转移合法性，非法抛 BusinessException */
    void checkTransition(String domain, Integer from, String event);
    /** 执行状态转移（仅校验+返回 to，不改 DB，由调用方落库） */
    Integer transition(String domain, Integer from, String event);
    /** 加载某域全部规则到 Redis（应用启动调用） */
    void loadRules(String domain);
    /** 刷新规则（DB 变更后调用） */
    void refreshRules(String domain);
}
```

### 7.2 规则存储

- 表：`system_state_machine`（domain / from_status / event / to_status / condition_expr / remark）
- 缓存：Redis Hash `dayan:sm:rule:{domain}`，field = `{from}:{event}`，value = `to_status`
- 加载时机：`@PostConstruct` 应用启动加载全部域；DB 变更通过 `refreshRules(domain)` 主动刷新

### 7.3 4 个状态机配置数据（P0 初始化）

| 状态机 | 域 | 状态数 | 关键转移 |
|--------|----|-------|---------|
| EQUITY_SM | equity | 8 | 0 库存→1 出库→2 激活→3 使用中→4 完成；→5 过期；→6 作废；7 更换权益人 |
| SERVICE_SESSION_SM | service | 7 + 子状态 | 1 待分配→2 待收集→3 方案中→4 安排中→5 服务中→6 完成；→7 取消；子状态 normal/hold/urgent/reassign/refund_review/refund_done/interrupted |
| ORDER_SM | order | 8 | 0 待支付→1 已支付→2 处理中→3 已完成；→4 取消；→5 退款中→6 已退款；→7 异常 |
| PARK_SM | park | 4 | 0 待审核→1 已上线→2 已下架；→3 暂停营业 |

具体转移规则按 v4.1 文档各域"状态机"章节填入 `state_machine_seed.sql`。

---

## 八、前端项目初始化设计

### 8.1 dayan-web-admin

- **构建**：Vite + Vue 3.4 + TypeScript
- **UI**：Element Plus 2.7 + SCSS
- **状态**：Pinia（user / permission / app 三个 store）
- **路由**：Vue Router 4，路由守卫前置（Token 校验 + 权限）
- **请求**：Axios 封装 `request.ts`，拦截器注入 `Admin-Token`、处理 10100/10101 跳登录页、`R<T>` 解包
- **布局**：`Layout.vue` 含侧边栏 + 顶栏 + 面包屑 + 标签页
- **页面**：`login/index.vue` + `dashboard/index.vue`（占位工作台）+ `404`
- **权限**：`v-permission` 指令骨架（P1 RBAC 联通后启用）
- **菜单**：从 `/admin-api/auth/info` 返回的菜单树动态渲染

### 8.2 dayan-web-channel

- 复用 Admin 模板（同 Vite + Vue3 + Element Plus）
- 调整：登录接口换 `/channel-api/auth/login`、Token 头 `Channel-Token`、菜单按渠道角色过滤
- P0 仅完成登录 + 布局 + 空工作台

### 8.3 dayan-miniprogram-agent（uni-app）

- uni-app 项目（Vue3 语法），同时支持微信小程序与 H5
- UI：uView UI 2.x
- 4 Tab 框架：首页 / 获客 / 客户 / 活动（P0 仅 Tab 壳 + 登录页）
- 登录页：手机号 + 选渠道（先调 `/agent-api/auth/channels`，再 `/agent-api/auth/login`）
- Pinia（uni 版）：user store 存 Token + channel_code
- 请求封装：uni.request 拦截器

### 8.4 dayan-miniprogram-client（uni-app）

- 同 Agent，4 Tab：首页 / 找机构 / 服务 / 我的
- 登录页：手机号 + 选渠道

---

## 九、CI/CD 设计（GitHub Actions）

### 9.1 `.github/workflows/ci.yml`

触发：push 到 main/dev、PR 到 main

```
jobs:
  backend-lint-test:
    - checkout
    - setup JDK 21
    - cache Maven
    - run: mvn -B -ntp verify（编译 + 单测 + 集成测试用 H2）
  frontend-build:
    - setup Node 20
    - npm ci in dayan-web-admin / dayan-web-channel
    - npm run build
  miniprogram-build:
    - npm ci in dayan-miniprogram-agent / client
    - npm run build:h5
```

### 9.2 `.github/workflows/docker.yml`

触发：push tag `v*` 或 push main

```
jobs:
  build-push:
    - matrix: [admin, channel, agent, client, supplier, distributor, gateway, job]
    - docker build -t ghcr.io/<owner>/dayan-<svc>:${{ github.sha }}
    - docker push ghcr.io/<owner>/dayan-<svc>:${{ github.sha }}
    - docker tag latest + push
    - 用 secrets.GITHUB_TOKEN 登录 GHCR
```

### 9.3 P0 不做的事

- 不强制单元测试覆盖率门禁（P1 再设 70%）
- 不接 SonarQube（无基础设施，留占位 workflow `sonar.yml` disabled）
- 不做多环境部署自动化（DEV/SIT/UAT/PROD 在 P11-P12 落地）

---

## 十、Docker Compose 编排设计

### 10.1 `docker-compose.infra.yml`（仅基础设施）

```yaml
services:
  mysql:
    image: mysql:8.0
    environment:
      MYSQL_ROOT_PASSWORD: root123
      MYSQL_DATABASE: dayan
    ports: ["3306:3306"]
    volumes:
      - mysql_data:/var/lib/mysql
      - ./db/migration:/docker-entrypoint-initdb.d
    command: --character-set-server=utf8mb4 --collation-server=utf8mb4_unicode_ci

  redis:
    image: redis:7.2-alpine
    ports: ["6379:6379"]

  nacos:
    image: nacos/nacos-server:v2.3.0
    environment: { MODE: standalone }
    ports: ["8848:8848", "9848:9848"]
```

### 10.2 `docker-compose.yml`（全量，含应用）

依赖 `docker-compose.infra.yml` 基础设施，追加 8 个应用服务（gateway + 6 端 + job），每个 `build` 对应 `docker/<svc>/Dockerfile`，`depends_on` 基础设施健康检查。

### 10.3 启动顺序

1. `docker compose -f docker-compose.infra.yml up -d`（先起 MySQL/Redis/Nacos）
2. MySQL 初始化完成后自动执行 `db/migration/*.sql`
3. `docker compose up -d --build`（构建并启动 8 个应用）
4. 健康检查：`/actuator/health` 各端口可访问

---

## 十一、P0 验收标准

| 维度 | 标准 |
|------|------|
| 后端启动 | 8 个进程（gateway + 6 端 + job）可独立启动，`/actuator/health` UP |
| 数据库 | `db/migration/*.sql` 执行后表数 = 127，种子数据齐全 |
| Admin 登录 | admin/admin123 登录成功，Token 正确返回，`/admin-api/auth/info` 可查 |
| Channel 登录 | channel_account 登录成功（P0 用种子数据） |
| Agent/Client 登录 | 选渠道流程走通：`/auth/channels` → 选定 → `/auth/login` 成功 |
| Sa-Token 隔离 | 四端 Token 命名空间独立，互不串扰 |
| 状态机引擎 | 4 个状态机规则加载到 Redis，`transition()` 对合法/非法转移判定正确 |
| 前端 Admin | 登录页 + 工作台可访问，登录后展示空布局 |
| 前端 Channel | 登录页可访问登录 |
| 小程序 Agent/Client | 4 Tab 框架启动，登录页可访问（H5 模式） |
| CI/CD | push 后 GitHub Actions lint + build 通过（本地 `act` 或真实 push 验证） |
| Docker | `docker compose up -d --build` 一键启动全部服务 |
| 租户隔离 | 跨 channel_code 数据隔离生效（A 渠道查不到 B 渠道数据） |

---

## 十二、工期与执行顺序（P0 内部排程）

| 日 | 任务 |
|----|------|
| D1 | 父 POM + dayan-common 全部子模块骨架 + 父级配置（JDK21/UTF-8/插件） |
| D1-D2 | dayan-common-core（R/异常/全局处理/雪花 ID/编码生成器/AES 工具） |
| D2 | dayan-common-mybatis（MyBatis-Plus 配置 + 租户拦截 + 自动填充） |
| D2 | dayan-common-security（Sa-Token 多端 + ContextFilter） |
| D2 | dayan-common-redis / -log / -swagger |
| D3 | 17 个业务模块骨架（pom + 分层目录 + Entity/Mapper 全量） |
| D3 | dayan-gateway + 6 启动模块 + dayan-job 骨架 |
| D3-D4 | 从 v4.1 抽取 127 表 DDL → `db/migration/01-17_*.sql` |
| D4 | 种子数据 SQL（字典/菜单/状态机/管理员） |
| D4 | docker-compose.infra.yml + 验证 DDL 全部执行通过 |
| D5 | 四端登录接口实现（organ/channel/agent/client account） |
| D5 | 选渠道特性 + 登录日志 + 失败锁定 |
| D5 | 状态机引擎实现 + 4 状态机配置数据校验 |
| D6 | dayan-web-admin 登录页 + 布局 + 路由守卫 |
| D6 | dayan-web-channel 复用模板 |
| D6-D7 | dayan-miniprogram-agent / client 4 Tab + 登录页 |
| D7 | Dockerfile（8 个）+ docker-compose.yml 全量 |
| D7 | GitHub Actions workflows（ci.yml + docker.yml） |
| D7 | README.md + 验收自查 |

---

## 十三、风险与缓解

| 风险 | 缓解 |
|------|------|
| 127 表 DDL 工作量超预期 | 按域分文件并行抽取；Entity 用 MyBatis-Plus 代码生成器辅助 |
| 四端登录选渠道逻辑复杂 | P0 微信 open_id 用 mock 模式，真实对接留 P2 |
| 状态机引擎设计不周 | P0 仅做"配置 + 校验"最小可用，复杂 condition_expr 留 P4/P5 |
| docker compose 一键启动慢 | 提供 infra-only 子文件，开发期只起基础设施，应用本地 IDE 跑 |
| GitHub Actions 免费额度 | 仅 push main 触发 docker.yml；PR 仅跑 ci.yml |

---

## 十四、后续阶段衔接

- **P1**：基于 P0 状态机引擎接入 RBAC（角色/权限/菜单 CRUD），Admin 系统管理页面全量
- **P2**：基于 P0 选渠道登录闭环，补 channel/agent/client 业务 CRUD
- **P3-P7**：各业务域 Controller 增量补全（P0 已建好 Entity/Mapper/Service 骨架）
- P0 的公共模块、网关、CI/CD、docker compose 在后续阶段持续复用，不返工
