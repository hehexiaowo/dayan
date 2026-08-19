# 你问我答——渠道补充知识库设计文档

- **日期**：2026-08-20
- **状态**：已批准，待实现
- **模块域**：dayan-module-tool（绑定表与合并逻辑）、dayan-module-system（可见性校验）、dayan-channel（控制台页面）、dayan-agent（无改动）

---

## 1. 概述

### 1.1 功能定位

你问我答（aichat）人物的知识库绑定目前只有一层：admin 在工具配置页绑定全局库（存 `tool_info.config_json.repoIds`），对所有渠道一视同仁。本期新增**渠道级补充**：渠道控制台可为每个你问我答人物补充自己可用的知识库，聊天时按"admin 全局库 ∪ 渠道补充库"合并检索。

### 1.2 核心规则

- **并集生效**：渠道有效知识库 = admin 全局 `repoIds` ∪ 渠道绑定表 repoIds（去重保序）；渠道只能加不能减。
- **补充范围**：渠道只能补充"自己渠道 + 后代渠道"名下的库（与渠道树视图范围一致），不含平台库。
- **配置粒度**：按人物分别补充（per-tool），与 admin 按人物配置的粒度一致。
- **平台库可见性**：admin 全局绑定的平台库（`channel_code` 为空）对渠道端放行，仅限 aichat 聊天路径；渠道控制台自己的知识库 chat/retrieve 维持现状（渠道仍不能直连平台库）。
- **agent 端**：前端零改动，聊天检索范围随渠道上下文自动不同。

---

## 2. 数据模型

### 2.1 `tool_channel_repo_bind` — 渠道人物知识库绑定

| 列名 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK AUTO | 主键 |
| tool_code | VARCHAR(50) NOT NULL | 你问我答人物（`tool_info.tool_code`，TL 前缀） |
| channel_code | VARCHAR(50) NOT NULL | 补充方渠道编码（一律从 `ContextHolder` 注入，不接收前端参数） |
| repo_id | BIGINT NOT NULL | 补充的知识库（`system_knowledge_repo.id`） |
| created_at / updated_at / creator / updater / deleted | — | 公共基础字段 |

约束：
- `UNIQUE KEY uk_tool_channel_repo (tool_code, channel_code, repo_id)`
- `KEY idx_channel_code (channel_code)`、`KEY idx_tool_code (tool_code)`

迁移文件：`db/migration/91_tool_channel_repo_bind.sql`。

---

## 3. 后端服务（dayan-module-tool）

### 3.1 绑定读写

新增 `ToolChannelRepoBind` 实体 + mapper + `ToolChannelRepoBindService`：

- `List<Long> listRepoIds(String toolCode, String channelCode)` — 读某渠道对某人物的补充；
- `void saveChannelRepos(String toolCode, String channelCode, List<Long> repoIds)` — **全量替换**：删除该 (toolCode, channelCode) 的旧行后插入新集合。保存前校验：
  1. 人物存在且 `tool_type = AI_QA`；
  2. 每个 repo 归属必须 ∈ 当前渠道自己 + 后代渠道（复用 `SystemKnowledgeRepoService.getRepoTree` 的渠道范围算法），否则 `BusinessException` 拒绝。

### 3.2 运行时合并

`ToolAichatChatServiceImpl.retrieveCitations` 改为：

```
有效 repoIds = admin config_json.repoIds ∪ tool_channel_repo_bind 查询结果
              （LinkedHashSet 去重保序）
```

合并逻辑封装在 `ToolChannelRepoBindService.mergeRepoIds(toolCode, globalRepoIds)`（内部按 `ContextHolder.getChannelCode()` 取渠道补充；无渠道上下文时退化为仅全局），`retrieveCitations` 与 `listQaPersonas` 两处复用。

### 3.3 可见性放开

`SystemKnowledgeRepoService` 新增 `requireRepoVisibleForPersona(Long id)`：

- 平台库（`channel_code` 为空）→ 直接放行（admin 全局绑定的平台库对所有渠道可用）；
- 渠道库 → 维持现有 `requireRepoVisible` 的归属/祖先/后代校验；
- 不存在 → NOT_FOUND。

仅 aichat 聊天路径调用；`ChannelKnowledgeController` 的 chat/retrieve 仍走 `requireRepoVisible`（平台库对渠道直查保持拒绝）。

### 3.4 agent 列表接口

`ToolInfoServiceImpl.listQaPersonas`（`/agent-api/tools/aichat/configs`）返回的 `repoIds` 改为渠道合并后的结果，保持"端上拿到的是本渠道有效库"语义。agent 前端不展示 repoIds，无前端改动。

---

## 4. 渠道控制台（dayan-channel）

### 4.1 后端接口

新 `ChannelToolAichatController`，放 `dayan-module-tool` 的 `controller/channel` 包（与 `controller/agent` 模式对齐），路径前缀 `/channel-api`：

| 接口 | 权限码 | 说明 |
|------|--------|------|
| `GET /tools/aichat/personas` | `channel:tool:aichat:view` | 启用中人物列表（名称/简介 + admin 全局 repoIds + 本渠道已补充 repoIds） |
| `GET /tools/aichat/repos/options` | `channel:tool:aichat:view` | 可补充的库（自己 + 后代渠道，不含平台库） |
| `PUT /tools/aichat/personas/{toolCode}/repos` | `channel:tool:aichat:update` | 保存补充（body: repoIds[]，全量替换） |

依赖调整：`dayan-starters/dayan-channel/pom.xml` 增加 `dayan-module-tool` 依赖（当前缺失）。

### 4.2 种子数据

- `db/migration/seed/menu_seed_channel.sql`：新增菜单 `channel_system_tool_aichat`（'问答人物'，`/system/tool-aichat`，component `tool/aichat/index`，权限码 `channel:tool:aichat:view`），挂在 `channel_system`（系统管理）目录下，与"知识仓库"并列；
- `db/migration/seed/channel_permission_seed.sql`：新增权限码 `channel:tool:aichat:view`（GET）与 `channel:tool:aichat:update`（PUT）。

### 4.3 前端页面

新页 `dayan-channel/src/views/tool/aichat/index.vue`：

- 人物表格：名称 / 简介 / admin 全局库标签（只读） / 本渠道补充库标签；
- 编辑弹窗：admin 全局库只读展示，补充库多选下拉（选项 = repos/options 返回，自己 + 后代渠道的库），留空 = 不补充（仅用 admin 全局）。

---

## 5. Agent 端（dayan-agent）

前端零改动。聊天行为自动按渠道合并结果检索：同一个人物在不同渠道的 agent 中引用不同知识库，即"每个渠道的实现对应的知识库不一样"。

---

## 6. 边界与错误处理

- 渠道保存时选到范围外库（非自己/后代名下）→ `BusinessException` 拒绝；
- 人物被 admin 停用/删除 → 渠道列表不再出现；已存绑定行保留（无害）；
- admin 删除库 → 绑定行残留；聊天时 `requireRepoVisibleForPersona` 对不存在的库抛 NOT_FOUND，被检索 try-catch 跳过（清理本期不做）；
- 无渠道上下文（admin 内部调 agent 接口）→ 合并退化为仅 admin 全局；
- 平台库放行仅限"人物绑定"路径，渠道直查平台库仍拒绝，不产生越权。

---

## 7. 测试

- 服务层：`saveChannelRepos` 全量替换；范围外 repo 拒绝；`requireRepoVisibleForPersona` 平台库放行 / 渠道库归属校验 / 不存在报错；
- 聊天合并：admin 全局 + 渠道补充去重保序；渠道上下文下平台库可检索；
- 接口层：personas 列表字段、repos/options 范围、保存接口权限与参数校验；
- 端到端：渠道页编辑保存 → agent 聊天引用渠道库内容。
