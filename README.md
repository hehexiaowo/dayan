# 大雁养老服务权益平台

> 养老服务权益全生命周期管理平台（B 端 4 端 + C 端 2 端），为保险代理人提供养老权益营销支撑：权益管理、机构库、内容获客、AI 创作。

## 技术栈

- **后端**：JDK 21 · Spring Boot 3.2 · Spring Cloud 2023（Nacos 注册配置）· MyBatis-Plus 3.5 · Sa-Token 1.39 多端隔离 · MySQL 8.0 · Redis 7.2
- **AI 能力**：阿里云百炼 qwen-plus（OpenAI 兼容网关，SSE 流式）· 百炼知识库 RAG · DashScope qwen-image-plus 文生图 · 凭据存 `system_config`（llm 分组，admin 系统配置页填写）
- **文件存储**：MinIO（`dayan-common-oss`，StorageService 抽象，DB 存纯 key，`/v1/files/preview/**` 代理展示）
- **前端**：dayan-admin / dayan-channel（Vue 3 + TypeScript + Element Plus）· dayan-agent / dayan-client（uni-app Vue3 H5/小程序，自研 Dy* 组件，无第三方 UI 库）
- **基础设施**：Docker · Docker Compose · GitHub Actions（CI + GHCR 镜像推送）

## 项目结构

```
dayan/
├── dayan-server/              后端聚合根（Maven 多模块）
│   ├── dayan-common/          公共模块（13 个：core/mybatis/security/log/redis/swagger/oss/sms/mq/pay/lbs/aliyun…）
│   ├── dayan-modules/         20 个业务域模块（organ/channel/park/equity/goods/order/agent/lead/content/knowledge/course…）
│   ├── dayan-starters/        6 个启动模块（admin/channel/agent/client/supplier/distributor，端口 8080-8085）
│   ├── dayan-gateway/         API 网关（8000）
│   ├── dayan-job/             定时任务模块（配额年度重置等）
│   └── pom.xml                后端父 POM
├── dayan-admin/              Admin 前端（Vue3 + Element Plus）
├── dayan-channel/            Channel 前端（Vue3 + Element Plus）
├── dayan-agent/              Agent 小程序/H5（uni-app，端口 517x dev）
├── dayan-client/             Client 小程序/H5（uni-app）
├── db/migration/              71 个迁移文件（DDL + 种子，Docker MySQL 启动自动执行）
├── docker/                    Dockerfile + docker-compose 编排（infra 基础设施 / 全量 8 服务）
├── docs/                      设计文档（01-09：需求/DB/架构/部署/API/规范/测试/计划/现状说明）
└── .superpowers/              AI 辅助开发规格与计划（本地工作区，gitignore 不入库）
```

## 快速启动

### 1. 启动基础设施（开发期中间件）

```bash
docker compose -f docker/docker-compose.infra.yml up -d
# MySQL（自动执行 db/migration 建表与种子）、Redis、Nacos、MinIO
```

### 2. 启动后端（开发期，按需起对应端）

```bash
cd dayan-server
mvn -pl dayan-starters/dayan-admin spring-boot:run     # Admin 8080
mvn -pl dayan-starters/dayan-channel spring-boot:run   # Channel 8081
mvn -pl dayan-starters/dayan-agent spring-boot:run     # Agent 8082
mvn -pl dayan-starters/dayan-client spring-boot:run    # Client 8083
# 改公共/业务模块后先 mvn -pl <模块> -am install -DskipTests 刷新 .m2，再重启对应 starter
```

### 3. 启动前端（开发期）

```bash
cd dayan-admin && npm run dev      # Admin http://localhost:5173
cd dayan-agent && npm run dev:h5   # Agent H5 http://localhost:517x
```

### 4. 一键全量（生产形态）

```bash
docker compose -f docker/docker-compose.yml up -d --build
```

## 功能全景（截至 2026-08）

| 域 | 已交付 |
|---|---|
| **组织与 RBAC** | 组织架构/角色/菜单/权限树，Sa-Token 多端隔离（admin/channel/agent/client），@SaCheckPermission 全端可用 |
| **渠道端** | 6 菜单组约 30 页全量（机构库/内容/商品/权益/客户/线索），渠道级能力闸（can_manage）与白名单配置 |
| **养老机构库** | 127 表级 DDL、20 家机构真实数据迁移（含结构化价格/展示板块/素材库）、三网络（活力/照护/旅居）独立页面体系、热力图地图（DataV + 天地图） |
| **权益体系** | 批量印刷→入库→销售→激活→履约全生命周期，goods_equity + service_item 架构，服务配额按年重置定时任务，激活码 DY+8 位 |
| **内容获客** | 内容多形态（图文/视频/图集/文件）、公众号样式列表、渠道可见性配置、营销海报、电子名片、分享体系（文章/海报/工具） |
| **AI 创作** | 六阶段交互式流水线（素材消化→策略+标题→大纲→正文→审计→润色打分→配图→成品），三目的（产品宣传/机构推荐/科普获客）、机构结构化素材源、RAG 知识库双库检索、qwen-image-plus 真实出图、反幻觉护栏（数据闭卷/审计关卡/合规用语） |
| **知识仓库** | 百炼 RAG 平台库+渠道库、文档上传解析导入、检索测试与问答、AI 创作素材勾选精准召回 |
| **课程体系** | 学习中心四板块（课程/内容/渠道/外部）、admin 4 tab 管理、5 态审核流、讲师管理 |
| **线索与客户** | 线索管理（公共池/认领/跟进）、客户关系、手机换绑、登录三方式（验证码/密码/微信骨架） |
| **文件上传** | MinIO 全量接入（admin 25 页 + 各端），DB 存纯 key，代理预览下载 |

## 测试账号与关键配置

| 端 | 账号 | 登录方式 | 说明 |
|---|---|---|---|
| Admin | admin / admin123 | 账号密码 | http://localhost:8080 |
| Channel | ch001 / admin123 | 账号密码 | http://localhost:8081，渠道 CH00001 |
| Agent | 手机号 13900000001（渠道 CH00001） | 验证码登录 或 密码 admin123 | 密码登录 identifier 用手机号 |
| Client | 13900000001 / 123456 | 密码登录（identifier=手机号 + channelCode=CH00001） | http://localhost:8083，对应客户端 CL0000000001 |

> 密码均为 BCrypt 哈希实测验证（2026-08-16）。验证码登录走 Mock 短信（`system_config` 未接真实短信商时，`/auth/sms/send` 响应 `data.devCode` 即验证码，60 秒冷却）。

- **AI 凭据**：`system_config` → `llm` 分组（api-key/api-host/access-key 等，admin 系统配置页填写；文生图与对话共用 api-key）
- **知识库**：admin 系统管理→知识仓库（平台库 + 每渠道一库），上传文档后需建库（百炼索引）
- **MinIO**：dayan / dayan12345，Console http://localhost:9001

## 文档索引

- 设计文档：`docs/01产品需求设计文档_v4.0.md` ~ `docs/09实现现状与变更说明.md`
- 开发规范：`docs/06项目开发规范.md`
- AI 开发工作区：`.superpowers/specs/`（设计规格）、`.superpowers/plans/`（实现计划）、`.superpowers/sdd/progress.md`（执行账本）——本地工作区，gitignore 不入库
- 数据库迁移：`db/migration/`（编号只增，`SET NAMES utf8mb4` 开头，种子幂等）
