# P2 渠道域 + 代理人域 + 客户域 - 设计规格

> **版本**：v1.0
> **编制日期**：2026-08-04
> **适用范围**：大雁养老 P2 阶段（渠道/代理人/客户域，后端核心优先）
> **依据**：`docs/08项目计划书.md` §2.3 P2、P0/P1 已完成基础
> **前置**：P0（24 表 Entity/Mapper + 四端登录）、P1（RBAC + 状态机 + 组织 + 账号 + 字典缓存）

---

## 一、范围（后端核心优先）

三域共 24 张表，P2 聚焦核心 CRUD + 渠道数据隔离验证：

| 序号 | 域 | 表数 | 核心内容 |
|------|----|----|---------|
| 1 | **渠道域（channel_）** | 11 | channel_info 渠道树 / channel_account 渠道账号 / channel_open_platform 开放平台（app_key/secret）/ channel_config_* 三类配置 |
| 2 | **代理人域（agent_）** | 6 | agent_info 代理人 / agent_account 账号 / agent_client_rel 客户绑定 / agent_favorite 收藏 / agent_performance 业绩 / agent_share_record 分享 |
| 3 | **客户域（client_）** | 7 | client_info 客户 / client_account 账号 / client_health_profile 健康档案 / client_care_need 照护需求 / client_family_member 家庭成员 / client_address 地址 / client_favorite 收藏 |

### P2 本阶段（必做）
- 三域核心 Entity 的 Service + Controller CRUD（渠道树/代理人/客户/账号/配置/档案）
- 渠道数据隔离验证（TenantLineHandler 对 channel_* / agent_* / client_* 表自动隔离）
- 代理人-客户绑定关系管理
- 客户健康档案 + 照护需求 + 家庭成员 + 地址 管理

### P2 后置
- channel_open_platform 真实签名验证（P2 只存配置，签名校验 P3+）
- channel_data_sync_log 数据同步（P3+）
- 开放平台接口文档/OAuth（P3+）
- 代理人海报生成/trace_id 归因（P6 内容域）
- 客户权益关联（P4 权益域）
- 各端前端页面（P8-P10 前端阶段）

---

## 二、渠道域设计

### 2.1 channel_info（渠道信息，5 类渠道树形）

- 树形结构（parent_code），5 类渠道（保险/银行/代理机构/其他）
- CRUD + 信用代码唯一校验
- 渠道是租户的"定义方"——channel_info 本身**不参与租户隔离**（已在 DayanTenantHandler 忽略清单），但其下数据（agent/client/account）按 channel_code 隔离

### 2.2 channel_account（渠道账号）

- 渠道端登录账号（P0 已实现 ChannelAuthService 登录）
- CRUD + 角色（channel_role/channel_permission，P2 简化：仅 CRUD 框架，RBAC 查询复用 organ 模式后置）

### 2.3 channel_open_platform（开放平台配置）

- app_key/app_secret 生成 + 回调地址 + IP 白名单
- app_secret AES-256-GCM 加密存储（用 common-core 的 AesGcmUtil）
- Token 认证 + 签名验证逻辑后置 P3

### 2.4 channel_config_*（三类配置）

- channel_config_content：内容配置（按 appType=agent/client 区分）
- channel_config_scene：场景配置
- channel_config_goods：商品配置
- 本质是渠道维度的白名单（哪些 content/scene/goods 对该渠道可见），CRUD 即可

---

## 三、代理人域设计

### 3.1 agent_info（代理人信息，按渠道隔离）

- 4 级等级（普通/银牌/金牌/钻石）
- 渠道归属绑定（channel_code）
- 编码 AG+5 位（CodeGenerator，渠道内唯一）
- 工号唯一校验

### 3.2 agent_account（代理人账号）

- P0 已实现 AgentAuthService（选渠道 + 登录）
- P2 补 CRUD（管理员视角管理代理人账号）

### 3.3 agent_client_rel（代理人-客户绑定）

- 绑定/解绑
- 同期唯一约束（同一代理人 + 同一客户 + 有效期不重叠）
- 关系有效期管理

### 3.4 其余（favorite/performance/share_record）

- 收藏/业绩/分享记录 CRUD（P2 基础 CRUD，业绩统计/分享归因 P6 补）

---

## 四、客户域设计

### 4.1 client_info（客户信息，按渠道隔离）

- 每渠道独立个体（client_code 渠道内唯一）
- 多类型（本人/家属/老人）
- 编码 CL+5 位

### 4.2 client_account（客户账号）

- P0 已实现 ClientAuthService（选渠道 + 登录）
- P2 补 CRUD

### 4.3 client_health_profile（健康档案）

- 一客户一档案
- 多类型健康信息（慢性病/过敏/手术/用药 JSON 字段）

### 4.4 client_care_need（照护需求评估）

- 5 类需求 + 优先级 + 预算 + 评估结果
- 关联管家评估（butler_code）

### 4.5 client_family_member / client_address

- 家庭成员 CRUD（同客户同关系唯一）
- 收货地址（≤20，默认地址标记）

---

## 五、渠道数据隔离验证（关键）

P0 已配置 TenantLineHandler（channel_code 字段级隔离），P2 验证：

1. **agent_info / agent_account / client_info / client_account 等分片表**：查询自动追加 `channel_code = ?`
2. **channel_info / organ_* 等平台共享表**：不隔离
3. **验证方式**：单测 mock ContextHolder.setChannelCode("CH00001")，查询生成的 SQL 含 channel_code 条件

各端 RBAC 扩展：
- Channel 端：DayanStpInterface 增加 channel 分支（channel_account → channel_role → channel_permission）
- Agent/Client 端：暂不需要细粒度权限（小程序端用登录态隔离即可）

---

## 六、P2 验收标准

| 维度 | 标准 |
|------|------|
| 渠道 CRUD | channel_info 树形 CRUD + channel_account CRUD + 开放平台配置（app_secret 加密） |
| 代理人 CRUD | agent_info CRUD + agent_client_rel 绑定/解绑 + 收藏/业绩/分享 |
| 客户 CRUD | client_info CRUD + 健康档案 + 照护需求 + 家庭成员 + 地址 |
| 渠道隔离 | 分片表查询自动带 channel_code；A 渠道查不到 B 渠道代理人/客户 |
| 编译 | 41 模块全量编译通过 |

---

## 七、任务拆分与执行

| 任务 | 内容 | 可并行 |
|------|------|--------|
| P2-A | 渠道域 Service+Controller（11 表核心） | ✅ |
| P2-B | 代理人域 Service+Controller（6 表） | ✅ |
| P2-C | 客户域 Service+Controller（7 表） | ✅ |
| P2-D | 渠道隔离验证 + Channel 端 RBAC | P2-A/B/C 后 |

三域在不同模块（channel/agent/client），互不冲突，可三路并行。
