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
├── dayan-admin/              Admin 前端（Vue3 + Element Plus）
├── dayan-channel/            Channel 前端（Vue3 + Element Plus）
├── dayan-agent/              Agent 小程序/H5（uni-app）
├── dayan-client/             Client 小程序/H5（uni-app）
├── db/migration/              数据库 DDL + 种子数据
├── docker/                    各服务 Dockerfile
├── docs/                      设计文档
└── .superpowers/              AI 辅助开发规格与计划（本地工作区，不入库）
```

## 快速启动（P0 完成后可用）

### 1. 启动基础设施

```bash
docker compose -f docker/docker-compose.infra.yml up -d
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
docker compose -f docker/docker-compose.yml up -d --build
```

## 开发规范

- 设计文档：`docs/01-08*.md`
- 开发规范：`docs/06项目开发规范.md`
- P0 设计规格：`.superpowers/specs/2026-08-04-p0-scaffold-design.md`

> 注：`.superpowers/` 为 AI 辅助开发的本地工作区（含规格、计划、SDD 报告），已通过 `.gitignore` 排除，不纳入版本管理；设计文档以 `docs/01-08*.md` 为准。

## 开发阶段

| 阶段 | 内容 | 状态 |
|------|------|------|
| P0 | 脚手架 & 基础设施 | ✅ 完成 |
| P1-P12 | 业务域开发 → 测试 → 上线 | ⏳ 待启动 |

## P0 完成内容

| 模块 | 内容 |
|------|------|
| **后端 common（11 模块）** | core/redis/mybatis/security/log/swagger 完整实现，142 单元测试通过；mq/oss/sms/pay/lbs 预留 |
| **17 业务域（127 表）** | Entity + Mapper 全量生成，编译通过 |
| **数据库 DDL（127 表）** | 17 域 SQL 全量，Docker MySQL 8.0 实测全部创建成功 |
| **种子数据** | 超管 admin/admin123 + 4 状态机(36 规则) + 44 字典 + 37 菜单 |
| **6 启动模块 + 网关 + job** | admin/channel/agent/client/supplier/distributor 独立启动，全量编译通过 |
| **四端登录接口** | Admin/Channel/Agent/Client 登录闭环，Sa-Token 多端隔离，Agent/Client 选渠道 |
| **状态机引擎** | 接口 + Redis 实现，6 测试通过 |
| **4 前端工程** | dayan-admin/channel（Vue3+Element Plus）+ dayan-agent/client（uni-app 小程序/H5） |
| **CI/CD** | GitHub Actions（ci.yml 编译测试 + docker.yml 8 服务推 GHCR） |
| **Docker 编排** | docker/docker-compose.infra.yml（基础设施）+ docker/docker-compose.yml（全量） |

## 验证 DDL（已实测）

```bash
# 启动 MySQL（注意 Git Bash 用 Windows 绝对路径或 MSYS_NO_PATHCONV=1）
docker run -d --name dayan-mysql-test -p 13306:3306 \
  -e MYSQL_ROOT_PASSWORD=root123 -e MYSQL_DATABASE=dayan \
  -v "F:/code/dayan/db/migration:/docker-entrypoint-initdb.d:ro" \
  mysql:8.0 --character-set-server=utf8mb4 --collation-server=utf8mb4_unicode_ci

# 验证表数 = 127
docker exec dayan-mysql-test mysql -uroot -proot123 dayan -e \
  "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='dayan';"

# 验证管理员
docker exec dayan-mysql-test mysql -uroot -proot123 dayan -e \
  "SELECT username, is_admin FROM organ_account;"
```

