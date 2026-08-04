# 大雁养老服务权益平台

> 养老服务权益全生命周期管理平台 · 单人全栈 + AI 辅助开发 · 18 周 13 阶段计划

## 技术栈

- **后端**：JDK 21 · Spring Boot 3.2 · Spring Cloud 2023 · Spring Cloud Alibaba 2023 · MyBatis-Plus 3.5 · Sa-Token 1.39 · MySQL 8.0 · Redis 7.2 · Nacos 2.3
- **前端**：Vue 3.4 + TypeScript + Vite + Element Plus（Admin/Channel） · uni-app + uView UI（Agent/Client 小程序/H5）
- **基础设施**：Docker · Docker Compose · GitHub Actions · GHCR

## 项目结构

```
dayan/
├── dayan-server/              后端聚合根（Maven 多模块）
│   ├── dayan-common/          公共模块（11 个子模块）
│   ├── dayan-modules/         17 个业务域模块
│   ├── dayan-starters/        6 个启动模块（admin/channel/agent/client/supplier/distributor）
│   ├── dayan-gateway/         API 网关
│   ├── dayan-job/             定时任务模块
│   └── pom.xml                后端父 POM
├── dayan-web-admin/           Admin 前端
├── dayan-web-channel/         Channel 前端
├── dayan-miniprogram-agent/   Agent 小程序/H5
├── dayan-miniprogram-client/  Client 小程序/H5
├── db/migration/              数据库 DDL + 种子数据
├── docker/                    各服务 Dockerfile
├── docs/                      设计文档
└── .superpowers/              AI 辅助开发规格与计划
```

## 快速启动（P0 完成后可用）

### 1. 启动基础设施

```bash
docker compose -f docker-compose.infra.yml up -d
```

启动 MySQL（自动执行 `db/migration/*.sql` 建表与初始化数据）、Redis、Nacos。

### 2. 本地运行后端（开发期）

```bash
mvn -B -ntp -DskipTests install
# 在 dayan-starters/dayan-admin 下：
mvn -B -ntp spring-boot:run
```

### 3. 一键启动全部服务（生产形态）

```bash
docker compose up -d --build
```

## 开发规范

- 设计文档：`docs/01-08*.md`
- 开发规范：`docs/06项目开发规范.md`
- P0 设计规格：`.superpowers/specs/2026-08-04-p0-scaffold-design.md`

## 开发阶段

| 阶段 | 内容 | 状态 |
|------|------|------|
| P0 | 脚手架 & 基础设施 | 🚧 进行中 |
| P1-P12 | 业务域开发 → 测试 → 上线 | ⏳ 待启动 |
