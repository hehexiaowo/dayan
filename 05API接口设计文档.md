---
AIGC:
    Label: "1"
    ContentProducer: 001191110102MACQD9K64018705
    ProduceID: 59831912238739_0/project_7654537080038572324-files/大雁养老_API接口设计文档.md
    ReservedCode1: ""
    ContentPropagator: 001191110102MACQD9K64028705
    PropagateID: 59831912238739#1785762039463
    ReservedCode2: ""
---
# 大雁养老 - API接口设计文档

> **版本**: v1.1  
> **编写日期**: 2026-08-03  
> **修订说明**: v1.1 对齐数据库 v4.2（settings/config 合并、订单状态统一8态、登录路径冲突修复、支付回调端修正、configs 接口族统一用 configCode）。v1.1 补充：API 端从四端对齐为七端，新增 `/supplier-api/`、`/distributor-api/` 独立前缀，供应商从 Channel 端分离，分销商独立成端。
> **文档状态**: 初版  
> **适用范围**: 大雁养老服务权益平台全业务域 API 接口设计

---

## 一、接口规范

### 1.1 RESTful 设计规范

| 规则 | 说明 | 示例 |
|------|------|------|
| URL 命名 | 小写字母 + 连字符，资源名使用名词复数 | `/admin-api/v1/park-infos` |
| 路径层级 | `/{端}-api/{版本}/{资源}/{操作}` | `/admin-api/v1/equity-depots/{code}/activate` |
| 资源路径 | 使用 `entity_code` 而非 `id` | `/admin-api/v1/park-infos/PK00001` |
| 路径参数例外 | 父级/主实体资源用 `{entityCode}`（如 `{parkCode}`、`{equityCode}`）；子表/明细表无独立业务编码时可用 `{id}`（雪花主键，后端通过 Long→String 序列化返回字符串） | `/admin-api/v1/park-infos/PK00001/rooms/{id}` |
| HTTP 方法 | GET=查询, POST=创建, PUT=全量更新, PATCH=部分更新, DELETE=删除。本项目实际约定：状态流转/单字段更新统一用 PUT（携带完整或部分字段）；PATCH 保留规范定义，当前不强制使用 | - |
| 分页参数 | `page_num`(页码,从1开始), `page_size`(每页条数,默认10,最大200) | `?page_num=1&page_size=20` |
| 游标分页 | `cursor`(上一页最后一条ID), 适用于大数据量场景 | `?cursor=123456&page_size=20` |
| 排序参数 | `sort_field`(排序字段), `sort_order`(asc/desc) | `?sort_field=created_at&sort_order=desc` |
| 过滤参数 | 查询字段直接拼 URL 参数 | `?status=1&city_code=310000&keyword=养老院` |
| 版本管理 | URL 路径内嵌版本号 `/v1/` | `/admin-api/v1/...` |
| 动作型子资源 | 资源自身状态流转/审批类业务动作用动词后缀（非 CRUD）；需成对保留 `GET /resource` 列表与 `POST /resource` 创建，动作仅作状态转移 | `POST /channel-api/v1/finance-invoices/apply`（渠道申请发票，业务动作，与 `POST /finance-invoices` 创建发票区分） |

### 1.2 统一响应格式

```json
{
  "code": 0,
  "message": "success",
  "data": {},
  "traceId": "a1b2c3d4e5f6"
}
```

**分页响应 data 结构**：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "list": [],
    "total": 100,
    "page_num": 1,
    "page_size": 20,
    "pages": 5
  },
  "traceId": "a1b2c3d4e5f6"
}
```

### 1.3 错误码规范

| 错误码范围 | 所属域 | 说明 |
|-----------|--------|------|
| 10000-10099 | 通用 | 参数校验错误 |
| 10100-10199 | 通用 | 认证/Token错误 |
| 10200-10299 | 通用 | 权限不足 |
| 10300-10399 | 通用 | 资源不存在/已删除 |
| 10400-10499 | 通用 | 业务规则校验失败 |
| 10500-10599 | 通用 | 系统内部错误 |
| 11000-11999 | 系统域（system） | 字典/状态机/配置/菜单/消息/日志/消息模板 |
| 12000-12999 | 核心域（organ） | 组织/部门/员工/账号/角色/权限 |
| 13000-13999 | 养老管家域（butler） | 管家/排班/评价/技能 |
| 14000-14999 | 供应商域（supplier） | 供应商/合同/评价/联系人（对应 /supplier-api/ 独立端） |
| 15000-15999 | 养老机构域（park） | 机构/房型/照护/餐饮/设施/媒体/顾问/周边配套/房费 |
| 16000-16999 | 场景域（scene） | 场景/项目/排期/资源 |
| 17000-17999 | 渠道域（channel） | 渠道/配置/对接/同步 |
| 18000-18999 | 代理人域（agent） | 代理人/收藏/业绩/分享 |
| 19000-19999 | 客户域（client） | 客户/健康档案/需求/家庭 |
| 20000-20999 | 权益域（equity） | 模板/批次/卡函/激活/使用人 |
| 21000-21999 | 服务域（service） | 会话/需求/方案/安排/回访 |
| 22000-22999 | 商品域（goods） | 商品/SKU |
| 23000-23999 | 内容域（content） | 内容/分类/媒体/分享/阅读 |
| 24000-24999 | 课程域（course） | 课程/讲师/学习记录 |
| 25000-25999 | 订单域（order） | 权益订单/场景订单/课程订单/旅居订单（资金往来迁入结算域 26000 段） |
| 26000-26999 | 结算域（finance） | 流水/结算单/发票/应收/对账 |
| 27000-27999 | 分销商域（distributor） | 分销商信息（对应 /distributor-api/ 独立端） |

### 1.4 认证与鉴权

**Sa-Token 多端 Token 隔离**：

| 端 | Token名称（Header） | 说明 |
|----|-------------------|------|
| Admin | `Authorization: Bearer {admin-token}` | organ/butler 账号 |
| Channel | `Authorization: Bearer {channel-token}` | channel 账号 |
| Supplier | `Authorization: Bearer {supplier-token}` | supplier 账号 |
| Distributor | `Authorization: Bearer {distributor-token}` | distributor 账号（预留：当前无 account 表） |
| Agent | `Authorization: Bearer {agent-token}` | agent 账号（按渠道隔离，Token 内含 channelCode） |
| Client | `Authorization: Bearer {client-token}` | client 账号（按渠道隔离，Token 内含 channelCode） |
| Open | `X-App-Key` + `X-Sign` + `X-Timestamp` | 签名认证 |

**接口权限注解**：

```java
@SaCheckPermission("system:dict:list")   // 接口级权限
@SaCheckRole("operations")               // 角色级权限
@SaCheckLogin                            // 仅需登录
```

**数据权限**：Channel/Agent 端接口自动注入 `channel_code` 过滤条件，Supplier 端自动注入 `supplier_code`，Distributor 端自动注入 `distributor_code`，各端数据相互独立；Admin 端按 `data_scope` 四级数据权限过滤。

### 1.5 端与路径前缀

| 端 | 路径前缀 | 账号体系 | 说明 |
|----|---------|---------|------|
| Admin 后台 | `/admin-api/` | organ_account / butler_account | 平台运营方 + 管家 |
| Channel 端 | `/channel-api/` | channel_account | 渠道（保险公司） |
| Supplier 端 | `/supplier-api/` | supplier_account | 供应商（前端暂不实现，API 预留） |
| Distributor 端 | `/distributor-api/` | distributor_info（当前无 account 表，预留） | 分销商（前端暂不实现，API 预留） |
| Agent 端 | `/agent-api/` | agent_account（按渠道隔离，同一代理人多渠道） | 保险代理人 |
| Client 端 | `/client-api/` | client_account（按渠道隔离，同一客户多渠道） | 客户/老人/家属 |
| 开放平台 | `/open-api/` | app_key 签名 | 外部系统对接 |

### 1.6 通用接口约定

| 约定项 | 规则 |
|--------|------|
| 时间格式 | `yyyy-MM-dd HH:mm:ss` |
| 日期格式 | `yyyy-MM-dd` |
| 金额单位 | 接口传输用 **分**（integer），数据库用 decimal(12,2) 元 |
| 文件上传 | `multipart/form-data`，单文件 ≤ 10MB，支持 jpg/png/pdf/mp4 |
| 批量操作 | 单次 ≤ 100 条 |
| 接口幂等 | 写接口需传 `requestId`（UUID），服务端去重 |
| 编码标识 | 对外接口统一使用 `{entity}_code` 业务编码，不暴露内部 `id` 主键 |
| 软删除 | DELETE 请求为逻辑删除，设置 `deleted_at` |
| 版本号 | 乐观锁字段 `version`，PUT 请求需携带 |
| 渠道上下文（Agent/Client 端） | 手机号/微信登录时，后端按手机号/open_id 跨渠道检索该身份关联的所有渠道账号；多渠道时返回渠道列表供用户选择（`GET /{端}-api/v1/auth/channels`），选定后 `POST /{端}-api/v1/auth/login` 携带 `channelCode` 完成登录，Token 内含 channelCode。同一代理人/客户可在多渠道各开一个账号（手机号/微信不要求全局唯一），跨渠道身份以手机号/open_id 自然键关联。代理人/客户亦可经 `ext_account_no`（渠道本身账号系统编码）登录 |

---

## 二、系统域接口（system_）— 18表

### 2.1 字典管理

#### 2.1.1 通用字典 system_dict_common

| 方法 | 路径 | 说明 | 权限标识 | 端 |
|------|------|------|---------|-----|
| GET | `/admin-api/v1/dict-commons` | 字典分页列表 | `system:dict:list` | admin |
| GET | `/admin-api/v1/dict-commons/tree` | 字典树形结构 | `system:dict:list` | admin |
| GET | `/admin-api/v1/dict-commons/{dictCode}` | 字典详情 | `system:dict:query` | admin |
| POST | `/admin-api/v1/dict-commons` | 新增字典 | `system:dict:create` | admin |
| PUT | `/admin-api/v1/dict-commons/{dictCode}` | 修改字典 | `system:dict:update` | admin |
| DELETE | `/admin-api/v1/dict-commons/{dictCode}` | 删除字典 | `system:dict:delete` | admin |
| GET | `/admin-api/v1/dict-commons/by-type/{dictType}` | 按类型查询字典列表 | `system:dict:list` | admin |
| POST | `/admin-api/v1/dict-commons/cache/refresh` | 刷新字典缓存 | `system:dict:manage` | admin |

#### 2.1.2 地域字典 system_dict_region

| 方法 | 路径 | 说明 | 权限标识 | 端 |
|------|------|------|---------|-----|
| GET | `/admin-api/v1/dict-regions/tree` | 省市区三级树形 | 登录即可 | admin/channel/agent/client |
| GET | `/admin-api/v1/dict-regions/children/{parentCode}` | 获取子级区划 | 登录即可 | 全端 |

#### 2.1.3 业务字典 system_dict_business

| 方法 | 路径 | 说明 | 权限标识 | 端 |
|------|------|------|---------|-----|
| GET | `/admin-api/v1/dict-bizs` | 业务字典分页列表 | `system:dict-biz:list` | admin |
| GET | `/admin-api/v1/dict-bizs/by-type/{dictType}` | 按类型查询 | 登录即可 | 全端 |
| GET | `/admin-api/v1/dict-bizs/by-domain/{domain}` | 按业务域查询 | `system:dict-biz:list` | admin |
| POST | `/admin-api/v1/dict-bizs` | 新增业务字典 | `system:dict-biz:create` | admin |
| PUT | `/admin-api/v1/dict-bizs/{dictCode}` | 修改业务字典 | `system:dict-biz:update` | admin |
| DELETE | `/admin-api/v1/dict-bizs/{dictCode}` | 删除业务字典 | `system:dict-biz:delete` | admin |

#### 2.1.4 IP 地域字典 system_dict_iplocation

| 方法 | 路径 | 说明 | 权限标识 | 端 |
|------|------|------|---------|-----|
| GET | `/admin-api/v1/dict-iplocations` | IP地域分页列表 | `system:dict:list` | admin |
| POST | `/admin-api/v1/dict-iplocations/import` | 批量导入IP数据 | `system:dict:import` | admin |
| GET | `/admin-api/v1/dict-iplocations/resolve` | IP解析地域 | `system:dict:list` | admin |

### 2.2 状态机管理

| 方法 | 路径 | 说明 | 权限标识 | 端 |
|------|------|------|---------|-----|
| GET | `/admin-api/v1/state-machines` | 状态机规则分页列表 | `system:sm:list` | admin |
| GET | `/admin-api/v1/state-machines/by-biz/{bizType}` | 按业务类型查询规则 | `system:sm:list` | admin |
| POST | `/admin-api/v1/state-machines` | 新增状态转移规则 | `system:sm:create` | admin |
| PUT | `/admin-api/v1/state-machines/{id}` | 修改状态转移规则 | `system:sm:update` | admin |
| DELETE | `/admin-api/v1/state-machines/{id}` | 删除状态转移规则 | `system:sm:delete` | admin |
| POST | `/admin-api/v1/state-machines/cache/refresh` | 刷新状态机缓存 | `system:sm:manage` | admin |

### 2.3 系统消息

| 方法 | 路径 | 说明 | 权限标识 | 端 |
|------|------|------|---------|-----|
| GET | `/admin-api/v1/messages` | 消息分页列表 | `system:message:list` | admin |
| GET | `/admin-api/v1/messages/{messageCode}` | 消息详情 | `system:message:query` | admin |
| POST | `/admin-api/v1/messages` | 发送系统消息 | `system:message:create` | admin |
| PUT | `/admin-api/v1/messages/{messageCode}` | 修改消息 | `system:message:update` | admin |
| DELETE | `/admin-api/v1/messages/{messageCode}` | 撤回/删除消息 | `system:message:delete` | admin |
| GET | `/admin-api/v1/messages/my-unread` | 我的未读消息 | 登录即可 | 全端 |
| PUT | `/admin-api/v1/messages/{messageCode}/read` | 标记已读 | 登录即可 | 全端 |
| PUT | `/admin-api/v1/messages/read-all` | 全部标记已读 | 登录即可 | 全端 |
| GET | `/admin-api/v1/messages/unread-count` | 未读消息数量 | 登录即可 | 全端 |

### 2.4 操作日志

| 方法 | 路径 | 说明 | 权限标识 | 端 |
|------|------|------|---------|-----|
| GET | `/admin-api/v1/operation-logs` | 操作日志分页列表 | `system:log:list` | admin |
| GET | `/admin-api/v1/operation-logs/{id}` | 日志详情 | `system:log:query` | admin |
| GET | `/admin-api/v1/organ-logs` | 核心日志 | `system:log-organ:list` | admin |
| GET | `/admin-api/v1/channel-logs` | 渠道日志 | `system:log-channel:list` | admin |
| GET | `/admin-api/v1/agent-logs` | 代理人日志 | `system:log-agent:list` | admin |
| GET | `/admin-api/v1/client-logs` | 客户日志 | `system:log-client:list` | admin |

### 2.5 系统配置

> 系统配置统一存储于 `system_config` 表，支持 global→organ→user 多级覆盖与运行时热更新；原 `system_settings` 已合并（v4.2）。配置变更记录写入 `system_config_log`。

| 方法 | 路径 | 说明 | 权限标识 | 端 |
|------|------|------|---------|-----|
| GET | `/admin-api/v1/configs` | 配置列表（支持按 group、scope、env 过滤） | `system:config:list` | admin |
| GET | `/admin-api/v1/configs/by-group/{group}` | 按分组获取配置（支持 scope 参数） | `system:config:list` | admin |
| GET | `/admin-api/v1/configs/{configCode}` | 配置详情 | `system:config:list` | admin |
| POST | `/admin-api/v1/configs` | 新增配置 | `system:config:create` | admin |
| PUT | `/admin-api/v1/configs/{configCode}` | 更新配置（单条，按 configCode） | `system:config:update` | admin |
| DELETE | `/admin-api/v1/configs/{configCode}` | 删除配置 | `system:config:delete` | admin |
| POST | `/admin-api/v1/configs/cache/refresh` | 刷新配置缓存 | `system:config:manage` | admin |
| GET | `/admin-api/v1/config-logs` | 配置变更历史查询（对应 system_config_log） | `system:config:list` | admin |

### 2.6 菜单管理

| 方法 | 路径 | 说明 | 权限标识 | 端 |
|------|------|------|---------|-----|
| GET | `/admin-api/v1/menus` | 菜单树形列表 | `system:menu:list` | admin |
| GET | `/admin-api/v1/menus/{menuCode}` | 菜单详情 | `system:menu:query` | admin |
| POST | `/admin-api/v1/menus` | 新增菜单 | `system:menu:create` | admin |
| PUT | `/admin-api/v1/menus/{menuCode}` | 修改菜单 | `system:menu:update` | admin |
| DELETE | `/admin-api/v1/menus/{menuCode}` | 删除菜单 | `system:menu:delete` | admin |
| GET | `/admin-api/v1/menus/user-tree` | 当前用户菜单树 | 登录即可 | 全端 |

### 2.7 消息模板（v4.2 从短信模板扩展，支持短信/站内信/推送/企微/微信模板消息/邮件全渠道）

> 模板字段说明：`channel_type` 支持 6 渠道（1=短信/2=站内信/3=APP推送/4=企微/5=微信模板消息/6=邮件）；渠道差异化配置（如短信签名、推送角标、企微 agentid、微信模板ID、邮件发件人等）统一收纳进 `channel_config`（JSON，非硬列）；`variables`（JSON）定义模板变量及其类型/校验规则；`fallback_channel_type` 指定主渠道失败后的降级渠道；`title`+`content` 为消息标题与正文（支持变量占位符 `${var}`）。

| 方法 | 路径 | 说明 | 权限标识 | 端 |
|------|------|------|---------|-----|
| GET | `/admin-api/v1/message-templates` | 消息模板分页列表（支持 channel_type/biz_type 过滤） | `system:message:list` | admin |
| GET | `/admin-api/v1/message-templates/{templateCode}` | 模板详情（含 channel_config、variables、fallback_channel_type） | `system:message:query` | admin |
| GET | `/admin-api/v1/message-templates/by-biz/{bizType}` | 按业务类型获取该事件的所有渠道模板组合（返回多渠道模板列表，用于发送时匹配） | `system:message:query` | admin |
| POST | `/admin-api/v1/message-templates` | 新增消息模板（必填 channel_type/title/content/biz_type，选填 channel_config(JSON)、variables(JSON)、fallback_channel_type） | `system:message:create` | admin |
| PUT | `/admin-api/v1/message-templates/{templateCode}` | 修改消息模板（可更新 channel_config、variables、fallback_channel_type 等） | `system:message:update` | admin |
| DELETE | `/admin-api/v1/message-templates/{templateCode}` | 删除消息模板 | `system:message:delete` | admin |
| POST | `/admin-api/v1/message-templates/test` | 发送测试消息（按 channel_type 走对应渠道，使用 channel_config 中的配置） | `system:message:test` | admin |

### 2.7.1 消息发送与查询

> 基于 `system_message` 发送记录表，提供消息发送、状态追踪、撤回与重试能力。发送时指定 `biz_type` + 接收人 + 变量参数，系统自动按 biz_type 匹配渠道模板组合，渲染变量后多渠道投递，并落库记录 `send_status`（0待发送/1发送中/2成功/3失败/4已送达/5已读/6已撤回）、`batch_code`（批次）、`provider_msg_id`（第三方回执）、`retry_count`、`error_msg`，支持失败后按 `fallback_channel_type` 降级重试。
>
> **与 §2.3 的关系**：下表 `GET /messages` 与 `GET /messages/{messageCode}` 复用 §2.3 已定义的同一端点（URL 与权限标识一致，底层为同一张 `system_message` 表）；本节侧重"发送记录"视角，列举发送相关的过滤维度（`channel_type`/`send_status`/`biz_type`/`target_code`）与发送详情字段（`provider_msg_id`/`error_msg`），不新增路径。其余 `send`/`revoke`/`retry`/`stats` 为本节独有的发送能力扩展。

| 方法 | 路径 | 说明 | 权限标识 | 端 |
|------|------|------|---------|-----|
| POST | `/admin-api/v1/messages/send` | 发送消息（指定 biz_type + 接收人 + 变量参数，系统自动匹配模板渲染并多渠道发送） | `system:message:send` | admin |
| GET | `/admin-api/v1/messages` | 消息发送记录列表（支持按 channel_type/send_status/biz_type/target_code 过滤） | `system:message:list` | admin |
| GET | `/admin-api/v1/messages/{messageCode}` | 消息发送详情（含发送状态、第三方回执 provider_msg_id、错误信息 error_msg） | `system:message:query` | admin |
| PUT | `/admin-api/v1/messages/{messageCode}/revoke` | 撤回消息（仅站内信 channel_type=2 支持，置 send_status=6） | `system:message:revoke` | admin |
| POST | `/admin-api/v1/messages/{messageCode}/retry` | 重试发送（失败的消息，retry_count+1，可按 fallback_channel_type 降级） | `system:message:retry` | admin |
| GET | `/admin-api/v1/messages/stats` | 消息发送统计（按渠道/状态/时间段汇总，用于监控大盘） | `system:message:stats` | admin |

---

## 三、核心域接口（organ_）— 9表

### 3.1 组织管理

| 方法 | 路径 | 说明 | 权限标识 | 端 |
|------|------|------|---------|-----|
| GET | `/admin-api/v1/organ-infos` | 组织分页列表 | `organ:info:list` | admin |
| GET | `/admin-api/v1/organ-infos/tree` | 组织树形结构 | `organ:info:list` | admin |
| GET | `/admin-api/v1/organ-infos/{organCode}` | 组织详情 | `organ:info:query` | admin |
| POST | `/admin-api/v1/organ-infos` | 新增组织 | `organ:info:create` | admin |
| PUT | `/admin-api/v1/organ-infos/{organCode}` | 修改组织 | `organ:info:update` | admin |
| DELETE | `/admin-api/v1/organ-infos/{organCode}` | 删除组织 | `organ:info:delete` | admin |

### 3.2 部门管理

| 方法 | 路径 | 说明 | 权限标识 | 端 |
|------|------|------|---------|-----|
| GET | `/admin-api/v1/departments` | 部门分页列表 | `organ:dept:list` | admin |
| GET | `/admin-api/v1/departments/tree` | 部门树形结构 | `organ:dept:list` | admin |
| GET | `/admin-api/v1/departments/{deptCode}` | 部门详情 | `organ:dept:query` | admin |
| POST | `/admin-api/v1/departments` | 新增部门 | `organ:dept:create` | admin |
| PUT | `/admin-api/v1/departments/{deptCode}` | 修改部门 | `organ:dept:update` | admin |
| DELETE | `/admin-api/v1/departments/{deptCode}` | 删除部门 | `organ:dept:delete` | admin |

### 3.3 员工管理

| 方法 | 路径 | 说明 | 权限标识 | 端 |
|------|------|------|---------|-----|
| GET | `/admin-api/v1/employees` | 员工分页列表 | `organ:employee:list` | admin |
| GET | `/admin-api/v1/employees/{employeeCode}` | 员工详情 | `organ:employee:query` | admin |
| POST | `/admin-api/v1/employees` | 新增员工 | `organ:employee:create` | admin |
| PUT | `/admin-api/v1/employees/{employeeCode}` | 修改员工 | `organ:employee:update` | admin |
| DELETE | `/admin-api/v1/employees/{employeeCode}` | 删除/离职 | `organ:employee:delete` | admin |
| PUT | `/admin-api/v1/employees/{employeeCode}/transfer` | 调岗 | `organ:employee:transfer` | admin |

### 3.4 账号管理

| 方法 | 路径 | 说明 | 权限标识 | 端 |
|------|------|------|---------|-----|
| POST | `/admin-api/v1/auth/login` | 管理员登录 | 无需认证 | admin |
| POST | `/admin-api/v1/auth/logout` | 退出登录 | 登录即可 | admin |
| POST | `/admin-api/v1/auth/refresh` | 刷新Token | 登录即可 | admin |
| GET | `/admin-api/v1/auth/user-info` | 获取当前用户信息 | 登录即可 | admin |
| PUT | `/admin-api/v1/auth/password` | 修改密码 | 登录即可 | admin |
| GET | `/admin-api/v1/accounts` | 账号分页列表 | `organ:account:list` | admin |
| POST | `/admin-api/v1/accounts` | 创建账号 | `organ:account:create` | admin |
| PUT | `/admin-api/v1/accounts/{accountCode}` | 修改账号 | `organ:account:update` | admin |
| PUT | `/admin-api/v1/accounts/{accountCode}/status` | 启用/禁用账号 | `organ:account:status` | admin |
| PUT | `/admin-api/v1/accounts/{accountCode}/reset-password` | 重置密码 | `organ:account:reset` | admin |

### 3.5 角色管理

| 方法 | 路径 | 说明 | 权限标识 | 端 |
|------|------|------|---------|-----|
| GET | `/admin-api/v1/roles` | 角色分页列表 | `organ:role:list` | admin |
| GET | `/admin-api/v1/roles/all` | 全部角色（下拉选） | `organ:role:list` | admin |
| GET | `/admin-api/v1/roles/{roleCode}` | 角色详情 | `organ:role:query` | admin |
| POST | `/admin-api/v1/roles` | 新增角色 | `organ:role:create` | admin |
| PUT | `/admin-api/v1/roles/{roleCode}` | 修改角色 | `organ:role:update` | admin |
| DELETE | `/admin-api/v1/roles/{roleCode}` | 删除角色 | `organ:role:delete` | admin |
| GET | `/admin-api/v1/roles/{roleCode}/permissions` | 角色权限列表 | `organ:role:query` | admin |
| PUT | `/admin-api/v1/roles/{roleCode}/permissions` | 批量设置角色权限 | `organ:role:permission` | admin |
| PUT | `/admin-api/v1/accounts/{accountCode}/roles` | 设置账号角色 | `organ:account:role` | admin |

### 3.6 权限管理

| 方法 | 路径 | 说明 | 权限标识 | 端 |
|------|------|------|---------|-----|
| GET | `/admin-api/v1/permissions` | 权限树形列表 | `organ:permission:list` | admin |
| POST | `/admin-api/v1/permissions` | 新增权限 | `organ:permission:create` | admin |
| PUT | `/admin-api/v1/permissions/{permissionCode}` | 修改权限 | `organ:permission:update` | admin |
| DELETE | `/admin-api/v1/permissions/{permissionCode}` | 删除权限 | `organ:permission:delete` | admin |

---

## 四、养老管家域接口（butler_）— 8表

| 方法 | 路径 | 说明 | 权限标识 | 端 |
|------|------|------|---------|-----|
| GET | `/admin-api/v1/butler-infos` | 管家分页列表 | `butler:info:list` | admin |
| GET | `/admin-api/v1/butler-infos/{butlerCode}` | 管家详情 | `butler:info:query` | admin |
| POST | `/admin-api/v1/butler-infos` | 新增管家 | `butler:info:create` | admin |
| PUT | `/admin-api/v1/butler-infos/{butlerCode}` | 修改管家 | `butler:info:update` | admin |
| DELETE | `/admin-api/v1/butler-infos/{butlerCode}` | 删除管家 | `butler:info:delete` | admin |
| GET | `/admin-api/v1/butler-infos/online` | 在线管家列表 | `butler:info:list` | admin |
| GET | `/admin-api/v1/butler-schedules` | 排班分页列表 | `butler:schedule:list` | admin |
| POST | `/admin-api/v1/butler-schedules/batch` | 批量设置排班 | `butler:schedule:create` | admin |
| PUT | `/admin-api/v1/butler-schedules/{id}` | 修改排班 | `butler:schedule:update` | admin |
| GET | `/admin-api/v1/butler-schedules/calendar/{butlerCode}` | 管家月历排班 | `butler:schedule:list` | admin |
| GET | `/admin-api/v1/butler-client-rels` | 管家客户关系列表 | `butler:rel:list` | admin |
| POST | `/admin-api/v1/butler-client-rels/bind` | 绑定管家客户 | `butler:rel:bind` | admin |
| PUT | `/admin-api/v1/butler-client-rels/{id}/unbind` | 解绑管家客户 | `butler:rel:unbind` | admin |
| GET | `/admin-api/v1/butler-service-records` | 服务记录分页列表 | `butler:record:list` | admin |
| POST | `/admin-api/v1/butler-service-records` | 新增服务记录 | `butler:record:create` | admin |
| PUT | `/admin-api/v1/butler-service-records/{id}` | 修改服务记录 | `butler:record:update` | admin |
| GET | `/admin-api/v1/butler-ratings` | 管家评价分页列表 | `butler:rating:list` | admin/channel/client |
| GET | `/admin-api/v1/butler-skills` | 管家技能列表 | `butler:skill:list` | admin |
| POST | `/admin-api/v1/butler-skills` | 新增技能标签 | `butler:skill:create` | admin |
| PUT | `/admin-api/v1/butler-skills/{id}` | 修改技能标签 | `butler:skill:update` | admin |
| DELETE | `/admin-api/v1/butler-skills/{id}` | 删除技能标签 | `butler:skill:delete` | admin |
| POST | `/admin-api/v1/butler-accounts/login` | 管家登录 | 无需认证 | admin |
| POST | `/admin-api/v1/butler-accounts/logout` | 管家退出 | 登录即可 | admin |

---

## 五、供应商域接口（supplier_）— 10表

| 方法 | 路径 | 说明 | 权限标识 | 端 |
|------|------|------|---------|-----|
| GET | `/admin-api/v1/supplier-infos` | 供应商分页列表 | `supplier:info:list` | admin\|supplier |
| GET | `/admin-api/v1/supplier-infos/{supplierCode}` | 供应商详情 | `supplier:info:query` | admin\|supplier |
| POST | `/admin-api/v1/supplier-infos` | 新增供应商 | `supplier:info:create` | admin\|supplier |
| PUT | `/admin-api/v1/supplier-infos/{supplierCode}` | 修改供应商 | `supplier:info:update` | admin\|supplier |
| DELETE | `/admin-api/v1/supplier-infos/{supplierCode}` | 删除供应商 | `supplier:info:delete` | admin |
| PUT | `/admin-api/v1/supplier-infos/{supplierCode}/audit` | 审核供应商 | `supplier:info:audit` | admin |
| GET | `/admin-api/v1/supplier-contracts` | 合同分页列表 | `supplier:contract:list` | admin\|supplier |
| POST | `/admin-api/v1/supplier-contracts` | 新增合同 | `supplier:contract:create` | admin\|supplier |
| PUT | `/admin-api/v1/supplier-contracts/{contractCode}` | 修改合同 | `supplier:contract:update` | admin\|supplier |
| PUT | `/admin-api/v1/supplier-contracts/{contractCode}/audit` | 审核合同 | `supplier:contract:audit` | admin |
| PUT | `/admin-api/v1/supplier-contracts/{contractCode}/renew` | 续约合同 | `supplier:contract:renew` | admin\|supplier |
| GET | `/admin-api/v1/supplier-evaluations` | 评价分页列表 | `supplier:eval:list` | admin\|supplier |
| POST | `/admin-api/v1/supplier-evaluations` | 新增评价 | `supplier:eval:create` | admin\|supplier |
| GET | `/admin-api/v1/supplier-contacts` | 联系人列表 | `supplier:contact:list` | admin\|supplier |
| POST | `/admin-api/v1/supplier-contacts` | 新增联系人 | `supplier:contact:create` | admin\|supplier |
| PUT | `/admin-api/v1/supplier-contacts/{id}` | 修改联系人 | `supplier:contact:update` | admin\|supplier |
| DELETE | `/admin-api/v1/supplier-contacts/{id}` | 删除联系人 | `supplier:contact:delete` | admin\|supplier |
| GET | `/admin-api/v1/supplier-open-platforms/{supplierCode}` | 开放平台配置 | `supplier:open:query` | admin\|supplier |
| PUT | `/admin-api/v1/supplier-open-platforms/{supplierCode}` | 更新开放平台配置 | `supplier:open:update` | admin\|supplier |
| POST | `/supplier-api/v1/auth/login` | 供应商登录 | 无需认证 | supplier |
| POST | `/supplier-api/v1/auth/logout` | 供应商退出 | 登录即可 | supplier |
| GET | `/supplier-api/v1/supplier-infos/{supplierCode}` | 供应商自身信息 | `supplier:self:query` | supplier |
| PUT | `/supplier-api/v1/supplier-infos/{supplierCode}` | 修改自身信息 | `supplier:self:update` | supplier |

---

## 六、养老机构域接口（park_）— 15表

| 方法 | 路径 | 说明 | 权限标识 | 端 |
|------|------|------|---------|-----|
| GET | `/admin-api/v1/park-infos` | 机构分页列表 | `park:info:list` | admin |
| GET | `/admin-api/v1/park-infos/{parkCode}` | 机构详情（含扩展信息） | `park:info:query` | admin/channel/agent/client |
| POST | `/admin-api/v1/park-infos` | 新增机构 | `park:info:create` | admin |
| PUT | `/admin-api/v1/park-infos/{parkCode}` | 修改机构基础信息 | `park:info:update` | admin |
| DELETE | `/admin-api/v1/park-infos/{parkCode}` | 删除机构 | `park:info:delete` | admin |
| PUT | `/admin-api/v1/park-infos/{parkCode}/publish` | 发布/下架机构 | `park:info:publish` | admin |
| PUT | `/admin-api/v1/park-infos/{parkCode}/audit` | 审核机构 | `park:info:audit` | admin |
| GET | `/admin-api/v1/park-media-images` | 图片列表 | `park:media:list` | admin |
| POST | `/admin-api/v1/park-media-images` | 上传图片 | `park:media:create` | admin |
| PUT | `/admin-api/v1/park-media-images/{id}` | 修改图片信息 | `park:media:update` | admin |
| DELETE | `/admin-api/v1/park-media-images/{id}` | 删除图片 | `park:media:delete` | admin |
| GET | `/admin-api/v1/park-media-videos` | 视频列表 | `park:media:list` | admin |
| POST | `/admin-api/v1/park-media-videos` | 上传视频 | `park:media:create` | admin |
| GET | `/admin-api/v1/park-media-files` | 文件列表 | `park:media:list` | admin |
| POST | `/admin-api/v1/park-media-files` | 上传文件 | `park:media:create` | admin |
| GET | `/admin-api/v1/park-media-vrs` | VR资源列表 | `park:media:list` | admin |
| POST | `/admin-api/v1/park-media-vrs` | 上传VR资源 | `park:media:create` | admin |
| GET | `/admin-api/v1/park-facilities` | 配套设施列表 | `park:facility:list` | admin |
| POST | `/admin-api/v1/park-facilities` | 新增设施 | `park:facility:create` | admin |
| PUT | `/admin-api/v1/park-facilities/{facilityCode}` | 修改设施 | `park:facility:update` | admin |
| DELETE | `/admin-api/v1/park-facilities/{facilityCode}` | 删除设施 | `park:facility:delete` | admin |
| GET | `/admin-api/v1/park-service-items` | 服务项目列表 | `park:service:list` | admin |
| POST | `/admin-api/v1/park-service-items` | 新增服务项目 | `park:service:create` | admin |
| PUT | `/admin-api/v1/park-service-items/{serviceCode}` | 修改服务项目 | `park:service:update` | admin |
| GET | `/admin-api/v1/park-advisers` | 顾问列表 | `park:adviser:list` | admin |
| POST | `/admin-api/v1/park-advisers` | 新增顾问 | `park:adviser:create` | admin |
| PUT | `/admin-api/v1/park-advisers/{id}` | 修改顾问 | `park:adviser:update` | admin |
| GET | `/admin-api/v1/park-peripheries` | 周边配套列表 | `park:periphery:list` | admin |
| POST | `/admin-api/v1/park-peripheries` | 新增周边配套 | `park:periphery:create` | admin |
| PUT | `/admin-api/v1/park-peripheries/{id}` | 修改周边配套 | `park:periphery:update` | admin |
| GET | `/admin-api/v1/park-room-types` | 房型列表 | `park:room:list` | admin/channel/client |
| POST | `/admin-api/v1/park-room-types` | 新增房型 | `park:room:create` | admin |
| PUT | `/admin-api/v1/park-room-types/{roomTypeCode}` | 修改房型 | `park:room:update` | admin |
| GET | `/admin-api/v1/park-room-prices` | 房费列表 | `park:price:list` | admin |
| POST | `/admin-api/v1/park-room-prices` | 新增房费 | `park:price:create` | admin |
| PUT | `/admin-api/v1/park-room-prices/{id}` | 修改房费 | `park:price:update` | admin |
| GET | `/admin-api/v1/park-care-types` | 照护类型列表 | `park:care:list` | admin/channel/client |
| POST | `/admin-api/v1/park-care-types` | 新增照护类型 | `park:care:create` | admin |
| GET | `/admin-api/v1/park-care-prices` | 照护费用列表 | `park:price:list` | admin |
| GET | `/admin-api/v1/park-food-types` | 餐饮类型列表 | `park:food:list` | admin |
| POST | `/admin-api/v1/park-food-types` | 新增餐饮类型 | `park:food:create` | admin |
| GET | `/admin-api/v1/park-food-prices` | 餐饮费用列表 | `park:price:list` | admin |
| GET | `/client-api/v1/park-infos` | 客户端机构列表（筛选+排序） | 登录即可 | client |
| GET | `/client-api/v1/park-infos/{parkCode}` | 客户端机构详情 | 登录即可 | client |
| GET | `/agent-api/v1/park-infos` | 代理人端机构列表 | 登录即可 | agent |
| GET | `/agent-api/v1/park-infos/{parkCode}` | 代理人端机构详情 | 登录即可 | agent |

---

## 七、场景域接口（scene_）— 5表

| 方法 | 路径 | 说明 | 权限标识 | 端 |
|------|------|------|---------|-----|
| GET | `/admin-api/v1/scene-infos` | 场景分页列表 | `scene:info:list` | admin |
| GET | `/admin-api/v1/scene-infos/{sceneCode}` | 场景详情 | `scene:info:query` | admin/channel/agent/client |
| POST | `/admin-api/v1/scene-infos` | 新增场景 | `scene:info:create` | admin |
| PUT | `/admin-api/v1/scene-infos/{sceneCode}` | 修改场景 | `scene:info:update` | admin |
| DELETE | `/admin-api/v1/scene-infos/{sceneCode}` | 删除场景 | `scene:info:delete` | admin |
| PUT | `/admin-api/v1/scene-infos/{sceneCode}/audit` | 审核场景 | `scene:info:audit` | admin |
| PUT | `/admin-api/v1/scene-infos/{sceneCode}/publish` | 上架/下架场景 | `scene:info:publish` | admin |
| GET | `/admin-api/v1/scene-items` | 场景项目列表 | `scene:item:list` | admin |
| POST | `/admin-api/v1/scene-items` | 新增场景项目 | `scene:item:create` | admin |
| PUT | `/admin-api/v1/scene-items/{itemCode}` | 修改场景项目 | `scene:item:update` | admin |
| GET | `/admin-api/v1/scene-item-prices` | 项目定价列表 | `scene:price:list` | admin |
| POST | `/admin-api/v1/scene-item-prices` | 新增项目定价 | `scene:price:create` | admin |
| GET | `/admin-api/v1/scene-schedules` | 排期分页列表 | `scene:schedule:list` | admin/channel/agent/client |
| POST | `/admin-api/v1/scene-schedules` | 新增排期 | `scene:schedule:create` | admin |
| PUT | `/admin-api/v1/scene-schedules/{id}` | 修改排期 | `scene:schedule:update` | admin |
| GET | `/admin-api/v1/scene-schedules/calendar/{sceneCode}` | 场景月历排期 | `scene:schedule:list` | admin/channel/agent/client |
| GET | `/admin-api/v1/scene-resources` | 场景资源列表 | `scene:resource:list` | admin |
| POST | `/admin-api/v1/scene-resources` | 新增场景资源 | `scene:resource:create` | admin |
| GET | `/agent-api/v1/scene-infos` | 代理人端场景列表 | 登录即可 | agent |
| GET | `/agent-api/v1/scene-infos/{sceneCode}` | 代理人端场景详情 | 登录即可 | agent |
| GET | `/client-api/v1/scene-infos` | 客户端场景列表 | 登录即可 | client |
| GET | `/client-api/v1/scene-infos/{sceneCode}` | 客户端场景详情 | 登录即可 | client |

---

## 八、渠道域接口（channel_）— 11表

| 方法 | 路径 | 说明 | 权限标识 | 端 |
|------|------|------|---------|-----|
| GET | `/admin-api/v1/channel-infos` | 渠道分页列表 | `channel:info:list` | admin |
| GET | `/admin-api/v1/channel-infos/{channelCode}` | 渠道详情 | `channel:info:query` | admin |
| POST | `/admin-api/v1/channel-infos` | 新增渠道 | `channel:info:create` | admin |
| PUT | `/admin-api/v1/channel-infos/{channelCode}` | 修改渠道 | `channel:info:update` | admin |
| DELETE | `/admin-api/v1/channel-infos/{channelCode}` | 删除渠道 | `channel:info:delete` | admin |
| PUT | `/admin-api/v1/channel-infos/{channelCode}/audit` | 审核渠道 | `channel:info:audit` | admin |
| GET | `/admin-api/v1/channel-infos/{channelCode}/dashboard` | 渠道数据看板 | `channel:dashboard:query` | admin |
| GET | `/admin-api/v1/channel-open-platforms/{channelCode}` | 开放平台配置 | `channel:open:query` | admin |
| PUT | `/admin-api/v1/channel-open-platforms/{channelCode}` | 更新开放平台配置 | `channel:open:update` | admin |
| GET | `/admin-api/v1/channel-config-contents` | 内容配置列表 | `channel:config:list` | admin/channel |
| POST | `/admin-api/v1/channel-config-contents` | 新增内容配置 | `channel:config:create` | admin/channel |
| PUT | `/admin-api/v1/channel-config-contents/{id}` | 修改内容配置 | `channel:config:update` | admin/channel |
| DELETE | `/admin-api/v1/channel-config-contents/{id}` | 删除内容配置 | `channel:config:delete` | admin/channel |
| GET | `/admin-api/v1/channel-config-scenes` | 场景配置列表 | `channel:config:list` | admin/channel |
| POST | `/admin-api/v1/channel-config-scenes` | 新增场景配置 | `channel:config:create` | admin/channel |
| GET | `/admin-api/v1/channel-config-goods` | 商品配置列表 | `channel:config:list` | admin/channel |
| POST | `/admin-api/v1/channel-config-goods` | 新增商品配置 | `channel:config:create` | admin/channel |
| GET | `/admin-api/v1/channel-data-sync-logs` | 同步日志列表 | `channel:sync:list` | admin |
| POST | `/admin-api/v1/channel-data-sync-logs/retry/{syncCode}` | 重试同步任务 | `channel:sync:retry` | admin |
| POST | `/channel-api/v1/auth/login` | 渠道账号登录 | 无需认证 | channel |
| POST | `/channel-api/v1/auth/logout` | 渠道退出 | 登录即可 | channel |
| GET | `/channel-api/v1/channel-infos/{channelCode}` | 本渠道信息 | `channel:self:query` | channel |
| PUT | `/channel-api/v1/channel-infos/{channelCode}` | 修改本渠道信息 | `channel:self:update` | channel |
| GET | `/channel-api/v1/channel-infos/tree` | 本渠道组织架构树 | `channel:self:tree` | channel |

---

## 九、代理人域接口（agent_）— 6表

| 方法 | 路径 | 说明 | 权限标识 | 端 |
|------|------|------|---------|-----|
| GET | `/admin-api/v1/agent-infos` | 代理人分页列表 | `agent:info:list` | admin/channel |
| GET | `/admin-api/v1/agent-infos/{agentCode}` | 代理人详情 | `agent:info:query` | admin/channel |
| POST | `/admin-api/v1/agent-infos` | 新增代理人 | `agent:info:create` | admin/channel |
| PUT | `/admin-api/v1/agent-infos/{agentCode}` | 修改代理人 | `agent:info:update` | admin/channel |
| DELETE | `/admin-api/v1/agent-infos/{agentCode}` | 删除代理人 | `agent:info:delete` | admin/channel |
| GET | `/admin-api/v1/agent-client-rels` | 代理人客户关系列表 | `agent:rel:list` | admin/channel |
| POST | `/admin-api/v1/agent-client-rels/bind` | 绑定代理人客户 | `agent:rel:bind` | admin/channel |
| PUT | `/admin-api/v1/agent-client-rels/{id}/unbind` | 解绑 | `agent:rel:unbind` | admin/channel |
| GET | `/admin-api/v1/agent-performances` | 业绩分页列表 | `agent:performance:list` | admin/channel |
| GET | `/admin-api/v1/agent-share-records` | 分享记录列表 | `agent:share:list` | admin/channel |
| GET | `/agent-api/v1/auth/channels` | 按手机号/open_id 检索关联渠道列表（登录前选渠道） | 无需认证 | agent |
| POST | `/agent-api/v1/auth/login` | 代理人登录（请求体携带 channelCode + 凭证；手机号/微信/ext_account_no） | 无需认证 | agent |
| POST | `/agent-api/v1/auth/logout` | 代理人退出 | 登录即可 | agent |
| POST | `/agent-api/v1/auth/wechat-login` | 微信登录（请求体携带 channelCode） | 无需认证 | agent |
| GET | `/agent-api/v1/agent-infos/me` | 我的信息 | 登录即可 | agent |
| PUT | `/agent-api/v1/agent-infos/me` | 修改我的信息 | 登录即可 | agent |
| GET | `/agent-api/v1/my-clients` | 我的客户列表 | 登录即可 | agent |
| GET | `/agent-api/v1/my-clients/{clientCode}` | 客户详情 | 登录即可 | agent |
| GET | `/agent-api/v1/my-favorites` | 我的收藏 | 登录即可 | agent |
| POST | `/agent-api/v1/my-favorites` | 添加收藏 | 登录即可 | agent |
| DELETE | `/agent-api/v1/my-favorites/{id}` | 取消收藏 | 登录即可 | agent |
| GET | `/agent-api/v1/my-performance` | 我的业绩 | 登录即可 | agent |
| GET | `/agent-api/v1/my-share-records` | 我的分享记录 | 登录即可 | agent |
| POST | `/agent-api/v1/my-share-records` | 创建分享记录 | 登录即可 | agent |
| GET | `/agent-api/v1/my-share-records/{shareCode}/stats` | 分享效果统计 | 登录即可 | agent |

---

## 十、客户域接口（client_）— 7表

| 方法 | 路径 | 说明 | 权限标识 | 端 |
|------|------|------|---------|-----|
| GET | `/admin-api/v1/client-infos` | 客户分页列表 | `client:info:list` | admin/channel |
| GET | `/admin-api/v1/client-infos/{clientCode}` | 客户详情 | `client:info:query` | admin/channel |
| POST | `/admin-api/v1/client-infos` | 新增客户（管家录入） | `client:info:create` | admin |
| PUT | `/admin-api/v1/client-infos/{clientCode}` | 修改客户信息 | `client:info:update` | admin/channel |
| DELETE | `/admin-api/v1/client-infos/{clientCode}` | 删除客户 | `client:info:delete` | admin |
| POST | `/admin-api/v1/client-infos/{clientCode}/assign-butler` | 分配管家 | `client:assign:butler` | admin |
| POST | `/admin-api/v1/client-infos/{clientCode}/assign-agent` | 分配代理人 | `client:assign:agent` | admin/channel |
| GET | `/admin-api/v1/client-health-profiles/{clientCode}` | 健康档案 | `client:health:query` | admin |
| PUT | `/admin-api/v1/client-health-profiles/{clientCode}` | 更新健康档案 | `client:health:update` | admin |
| GET | `/admin-api/v1/client-care-needs` | 照护需求列表 | `client:care:list` | admin |
| POST | `/admin-api/v1/client-care-needs` | 新增照护需求评估 | `client:care:create` | admin |
| PUT | `/admin-api/v1/client-care-needs/{id}` | 修改照护需求 | `client:care:update` | admin |
| GET | `/admin-api/v1/client-family-members` | 家庭成员列表 | `client:family:list` | admin |
| POST | `/admin-api/v1/client-family-members` | 新增家庭成员 | `client:family:create` | admin |
| PUT | `/admin-api/v1/client-family-members/{id}` | 修改家庭成员 | `client:family:update` | admin |
| GET | `/client-api/v1/auth/channels` | 按手机号/open_id 检索关联渠道列表（登录前选渠道） | 无需认证 | client |
| POST | `/client-api/v1/auth/login` | 客户登录（请求体携带 channelCode + 凭证；手机号/微信/ext_account_no） | 无需认证 | client |
| POST | `/client-api/v1/auth/logout` | 客户退出 | 登录即可 | client |
| POST | `/client-api/v1/auth/wechat-login` | 微信登录（请求体携带 channelCode） | 无需认证 | client |
| POST | `/client-api/v1/auth/sms-login` | 短信验证码登录（请求体携带 channelCode） | 无需认证 | client |
| GET | `/client-api/v1/client-infos/me` | 我的信息 | 登录即可 | client |
| PUT | `/client-api/v1/client-infos/me` | 修改我的信息 | 登录即可 | client |
| GET | `/client-api/v1/my-health-profile` | 我的健康档案 | 登录即可 | client |
| PUT | `/client-api/v1/my-health-profile` | 更新我的健康档案 | 登录即可 | client |
| GET | `/client-api/v1/my-family-members` | 我的家庭成员 | 登录即可 | client |
| POST | `/client-api/v1/my-family-members` | 新增家庭成员 | 登录即可 | client |
| PUT | `/client-api/v1/my-family-members/{id}` | 修改家庭成员 | 登录即可 | client |
| GET | `/client-api/v1/my-addresses` | 我的收货地址 | 登录即可 | client |
| POST | `/client-api/v1/my-addresses` | 新增收货地址 | 登录即可 | client |
| PUT | `/client-api/v1/my-addresses/{id}` | 修改收货地址 | 登录即可 | client |
| DELETE | `/client-api/v1/my-addresses/{id}` | 删除收货地址 | 登录即可 | client |
| GET | `/client-api/v1/my-favorites` | 我的收藏 | 登录即可 | client |
| POST | `/client-api/v1/my-favorites` | 添加收藏 | 登录即可 | client |
| DELETE | `/client-api/v1/my-favorites/{id}` | 取消收藏 | 登录即可 | client |

---

## 十一、权益域接口（equity_）— 6表

| 方法 | 路径 | 说明 | 权限标识 | 端 |
|------|------|------|---------|-----|
| GET | `/admin-api/v1/equity-templates` | 权益模板分页列表 | `equity:template:list` | admin |
| GET | `/admin-api/v1/equity-templates/{templateCode}` | 模板详情 | `equity:template:query` | admin |
| POST | `/admin-api/v1/equity-templates` | 新增权益模板 | `equity:template:create` | admin |
| PUT | `/admin-api/v1/equity-templates/{templateCode}` | 修改权益模板 | `equity:template:update` | admin |
| DELETE | `/admin-api/v1/equity-templates/{templateCode}` | 删除权益模板 | `equity:template:delete` | admin |
| GET | `/admin-api/v1/equity-batches` | 批次分页列表 | `equity:batch:list` | admin |
| GET | `/admin-api/v1/equity-batches/{batchCode}` | 批次详情 | `equity:batch:query` | admin |
| POST | `/admin-api/v1/equity-batches` | 新增批次 | `equity:batch:create` | admin |
| POST | `/admin-api/v1/equity-batches/{batchCode}/produce` | 生产权益卡 | `equity:batch:produce` | admin |
| PUT | `/admin-api/v1/equity-batches/{batchCode}/allocate` | 分配至渠道 | `equity:batch:allocate` | admin |
| GET | `/admin-api/v1/equity-depots` | 权益卡/函分页列表 | `equity:depot:list` | admin/channel |
| GET | `/admin-api/v1/equity-depots/{equityCode}` | 权益卡详情 | `equity:depot:query` | admin/channel |
| POST | `/admin-api/v1/equity-depots/{equityCode}/outbound` | 出库（寄送） | `equity:depot:outbound` | admin |
| PUT | `/admin-api/v1/equity-depots/{equityCode}/void` | 作废权益 | `equity:depot:void` | admin |
| GET | `/admin-api/v1/equity-depots/search` | 综合查询（按equity_code） | `equity:depot:query` | admin |
| GET | `/admin-api/v1/equity-activates` | 激活记录分页列表 | `equity:activate:list` | admin/channel |
| GET | `/admin-api/v1/equity-activates/{activateCode}` | 激活详情 | `equity:activate:query` | admin/channel |
| GET | `/admin-api/v1/equity-use-persons/{equityCode}` | 使用人列表 | `equity:person:list` | admin |
| POST | `/admin-api/v1/equity-use-persons` | 新增使用人 | `equity:person:create` | admin |
| PUT | `/admin-api/v1/equity-use-persons/{id}` | 修改使用人 | `equity:person:update` | admin |
| GET | `/admin-api/v1/equity-change-holders` | 更换权益人记录 | `equity:change:list` | admin |
| POST | `/admin-api/v1/equity-change-holders` | 发起更换权益人 | `equity:change:create` | admin |
| PUT | `/admin-api/v1/equity-change-holders/{id}/complete` | 完成更换 | `equity:change:complete` | admin |
| PUT | `/admin-api/v1/equity-change-holders/{id}/rollback` | 回滚更换 | `equity:change:rollback` | admin |
| POST | `/client-api/v1/equity-activates` | 客户端激活权益 | 登录即可 | client |
| POST | `/client-api/v1/equity-activates/verify` | 验证激活码 | 登录即可 | client |
| GET | `/client-api/v1/my-equities` | 我的权益列表 | 登录即可 | client |
| GET | `/client-api/v1/my-equities/{equityCode}` | 权益详情 | 登录即可 | client |
| GET | `/agent-api/v1/my-equities` | 我的权益列表（代理人赠送的） | 登录即可 | agent |

---

## 十二、服务域接口（service_）— 7表

| 方法 | 路径 | 说明 | 权限标识 | 端 |
|------|------|------|---------|-----|
| GET | `/admin-api/v1/service-sessions` | 服务会话分页列表 | `service:session:list` | admin |
| GET | `/admin-api/v1/service-sessions/{sessionCode}` | 会话详情 | `service:session:query` | admin |
| POST | `/admin-api/v1/service-sessions` | 创建服务会话 | `service:session:create` | admin |
| PUT | `/admin-api/v1/service-sessions/{sessionCode}/assign` | 分配管家 | `service:session:assign` | admin |
| PUT | `/admin-api/v1/service-sessions/{sessionCode}/transition` | 状态转移 | `service:session:transition` | admin |
| GET | `/admin-api/v1/service-sessions/{sessionCode}/timeline` | 服务时间线 | `service:session:query` | admin |
| GET | `/admin-api/v1/service-equity-demands` | 需求收集分页列表 | `service:demand:list` | admin |
| GET | `/admin-api/v1/service-equity-demands/{id}` | 需求详情 | `service:demand:query` | admin |
| POST | `/admin-api/v1/service-equity-demands` | 新增需求记录 | `service:demand:create` | admin |
| PUT | `/admin-api/v1/service-equity-demands/{id}` | 修改需求记录 | `service:demand:update` | admin |
| GET | `/admin-api/v1/service-equity-solutions` | 方案定制分页列表 | `service:solution:list` | admin |
| GET | `/admin-api/v1/service-equity-solutions/{solutionCode}` | 方案详情 | `service:solution:query` | admin |
| POST | `/admin-api/v1/service-equity-solutions` | 新增方案 | `service:solution:create` | admin |
| PUT | `/admin-api/v1/service-equity-solutions/{solutionCode}` | 修改方案 | `service:solution:update` | admin |
| PUT | `/admin-api/v1/service-equity-solutions/{solutionCode}/confirm` | 确认方案 | `service:solution:confirm` | admin |
| PUT | `/admin-api/v1/service-equity-solutions/{solutionCode}/reject` | 驳回方案 | `service:solution:reject` | admin |
| GET | `/admin-api/v1/service-equity-arranges` | 全程安排分页列表 | `service:arrange:list` | admin |
| POST | `/admin-api/v1/service-equity-arranges` | 新增安排 | `service:arrange:create` | admin |
| PUT | `/admin-api/v1/service-equity-arranges/{arrangeCode}` | 修改安排 | `service:arrange:update` | admin |
| PUT | `/admin-api/v1/service-equity-arranges/{arrangeCode}/confirm` | 确认安排 | `service:arrange:confirm` | admin |
| PUT | `/admin-api/v1/service-equity-arranges/{arrangeCode}/complete` | 完成安排 | `service:arrange:complete` | admin |
| GET | `/admin-api/v1/service-equity-followups` | 回访品控分页列表 | `service:followup:list` | admin |
| POST | `/admin-api/v1/service-equity-followups` | 新增回访记录 | `service:followup:create` | admin |
| PUT | `/admin-api/v1/service-equity-followups/{followupCode}` | 修改回访记录 | `service:followup:update` | admin |
| GET | `/admin-api/v1/service-evaluations` | 服务评价列表 | `service:eval:list` | admin/channel/client |
| POST | `/client-api/v1/service-evaluations` | 客户提交评价 | 登录即可 | client |
| GET | `/admin-api/v1/service-visit-records` | 探访记录列表 | `service:visit:list` | admin |
| POST | `/admin-api/v1/service-visit-records` | 新增探访记录 | `service:visit:create` | admin |
| GET | `/admin-api/v1/service-change-logs/{sessionCode}` | 变更日志 | `service:log:list` | admin |
| GET | `/client-api/v1/my-service-sessions` | 我的服务会话列表 | 登录即可 | client |
| GET | `/client-api/v1/my-service-sessions/{sessionCode}` | 服务进度详情 | 登录即可 | client |
| GET | `/client-api/v1/my-service-sessions/{sessionCode}/timeline` | 服务时间线 | 登录即可 | client |

---

## 十三、商品域接口（goods_）— 5表

| 方法 | 路径 | 说明 | 权限标识 | 端 |
|------|------|------|---------|-----|
| GET | `/admin-api/v1/goods-infos` | 商品分页列表 | `goods:info:list` | admin |
| GET | `/admin-api/v1/goods-infos/{goodsCode}` | 商品详情 | `goods:info:query` | admin/channel |
| POST | `/admin-api/v1/goods-infos` | 新增商品 | `goods:info:create` | admin |
| PUT | `/admin-api/v1/goods-infos/{goodsCode}` | 修改商品 | `goods:info:update` | admin |
| DELETE | `/admin-api/v1/goods-infos/{goodsCode}` | 删除商品 | `goods:info:delete` | admin |
| PUT | `/admin-api/v1/goods-infos/{goodsCode}/publish` | 上架/下架商品 | `goods:info:publish` | admin |
| PUT | `/admin-api/v1/goods-infos/{goodsCode}/audit` | 审核商品 | `goods:info:audit` | admin |
| GET | `/admin-api/v1/goods-sku-equities` | 权益SKU列表 | `goods:sku:list` | admin |
| POST | `/admin-api/v1/goods-sku-equities` | 新增权益SKU | `goods:sku:create` | admin |
| PUT | `/admin-api/v1/goods-sku-equities/{skuCode}` | 修改权益SKU | `goods:sku:update` | admin |
| GET | `/admin-api/v1/goods-sku-scenes` | 场景SKU列表 | `goods:sku:list` | admin |
| POST | `/admin-api/v1/goods-sku-scenes` | 新增场景SKU | `goods:sku:create` | admin |
| GET | `/admin-api/v1/goods-sku-courses` | 课程SKU列表 | `goods:sku:list` | admin |
| POST | `/admin-api/v1/goods-sku-courses` | 新增课程SKU | `goods:sku:create` | admin |
| GET | `/admin-api/v1/goods-sku-sojourns` | 旅居SKU列表 | `goods:sku:list` | admin |
| POST | `/admin-api/v1/goods-sku-sojourns` | 新增旅居SKU | `goods:sku:create` | admin |
| GET | `/channel-api/v1/goods-infos` | 渠道可购商品列表 | 登录即可 | channel |
| GET | `/channel-api/v1/goods-infos/{goodsCode}` | 商品详情 | 登录即可 | channel |

---

## 十四、内容域接口（content_）— 5表

| 方法 | 路径 | 说明 | 权限标识 | 端 |
|------|------|------|---------|-----|
| GET | `/admin-api/v1/content-infos` | 内容分页列表 | `content:info:list` | admin |
| GET | `/admin-api/v1/content-infos/{contentCode}` | 内容详情 | `content:info:query` | admin |
| POST | `/admin-api/v1/content-infos` | 新增内容 | `content:info:create` | admin |
| PUT | `/admin-api/v1/content-infos/{contentCode}` | 修改内容 | `content:info:update` | admin |
| DELETE | `/admin-api/v1/content-infos/{contentCode}` | 删除内容 | `content:info:delete` | admin |
| PUT | `/admin-api/v1/content-infos/{contentCode}/publish` | 发布/下架内容 | `content:info:publish` | admin |
| PUT | `/admin-api/v1/content-infos/{contentCode}/audit` | 审核内容 | `content:info:audit` | admin |
| GET | `/admin-api/v1/content-categories` | 内容分类树形列表 | `content:category:list` | admin |
| POST | `/admin-api/v1/content-categories` | 新增分类 | `content:category:create` | admin |
| PUT | `/admin-api/v1/content-categories/{categoryCode}` | 修改分类 | `content:category:update` | admin |
| DELETE | `/admin-api/v1/content-categories/{categoryCode}` | 删除分类 | `content:category:delete` | admin |
| GET | `/admin-api/v1/content-media` | 媒体资源列表 | `content:media:list` | admin |
| POST | `/admin-api/v1/content-media` | 上传媒体 | `content:media:create` | admin |
| DELETE | `/admin-api/v1/content-media/{id}` | 删除媒体 | `content:media:delete` | admin |
| GET | `/admin-api/v1/content-record-shares` | 分享记录列表 | `content:record:list` | admin |
| GET | `/admin-api/v1/content-record-reads` | 阅读记录列表 | `content:record:list` | admin |
| GET | `/admin-api/v1/content-record-reads/stats/{contentCode}` | 内容UV/PV统计 | `content:record:stats` | admin |
| GET | `/agent-api/v1/content-infos` | 代理人端内容列表 | 登录即可 | agent |
| GET | `/agent-api/v1/content-infos/{contentCode}` | 内容详情 | 登录即可 | agent |
| GET | `/client-api/v1/content-infos` | 客户端内容列表 | 登录即可 | client |
| GET | `/client-api/v1/content-infos/{contentCode}` | 内容详情 | 登录即可 | client |
| POST | `/agent-api/v1/content-shares` | 代理人分享内容 | 登录即可 | agent |
| POST | `/client-api/v1/content-reads` | 记录阅读 | 登录即可 | client |

---

## 十五、课程域接口（course_）— 3表

| 方法 | 路径 | 说明 | 权限标识 | 端 |
|------|------|------|---------|-----|
| GET | `/admin-api/v1/course-infos` | 课程分页列表 | `course:info:list` | admin |
| GET | `/admin-api/v1/course-infos/{courseCode}` | 课程详情 | `course:info:query` | admin |
| POST | `/admin-api/v1/course-infos` | 新增课程 | `course:info:create` | admin |
| PUT | `/admin-api/v1/course-infos/{courseCode}` | 修改课程 | `course:info:update` | admin |
| DELETE | `/admin-api/v1/course-infos/{courseCode}` | 删除课程 | `course:info:delete` | admin |
| PUT | `/admin-api/v1/course-infos/{courseCode}/publish` | 上架/下架 | `course:info:publish` | admin |
| GET | `/admin-api/v1/course-lecturers` | 讲师分页列表 | `course:lecturer:list` | admin |
| POST | `/admin-api/v1/course-lecturers` | 新增讲师 | `course:lecturer:create` | admin |
| PUT | `/admin-api/v1/course-lecturers/{lecturerCode}` | 修改讲师 | `course:lecturer:update` | admin |
| GET | `/admin-api/v1/course-record-learns` | 学习记录列表 | `course:learn:list` | admin |
| GET | `/agent-api/v1/course-infos` | 代理人端课程列表 | 登录即可 | agent |
| GET | `/agent-api/v1/course-infos/{courseCode}` | 课程详情 | 登录即可 | agent |
| GET | `/agent-api/v1/my-learn-records` | 我的学习记录 | 登录即可 | agent |
| GET | `/client-api/v1/course-infos` | 客户端课程列表 | 登录即可 | client |
| GET | `/client-api/v1/course-infos/{courseCode}` | 课程详情 | 登录即可 | client |
| GET | `/client-api/v1/my-learn-records` | 我的学习记录 | 登录即可 | client |

---

## 十六、订单域接口（order_）— 4表（v4.2 支付/退款迁入结算域，状态日志迁入系统域）

| 方法 | 路径 | 说明 | 权限标识 | 端 |
|------|------|------|---------|-----|
| GET | `/admin-api/v1/order-equities` | 权益订单分页列表 | `order:equity:list` | admin |
| GET | `/admin-api/v1/order-equities/{orderCode}` | 订单详情 | `order:equity:query` | admin |
| POST | `/admin-api/v1/order-equities` | 创建权益订单 | `order:equity:create` | admin |
| PUT | `/admin-api/v1/order-equities/{orderCode}/cancel` | 取消订单 | `order:equity:cancel` | admin |
| POST | `/admin-api/v1/order-equities/{orderCode}/deliver` | 权益入库卡库 | `order:equity:deliver` | admin |
| GET | `/channel-api/v1/order-equities` | 本渠道权益订单 | 登录即可 | channel |
| POST | `/channel-api/v1/order-equities` | 渠道下单采购 | 登录即可 | channel |
| GET | `/agent-api/v1/order-equities` | 我的权益订单 | 登录即可 | agent |
| POST | `/agent-api/v1/order-equities` | 代理人下单采购 | 登录即可 | agent |
| GET | `/admin-api/v1/order-scenes` | 场景订单分页列表 | `order:scene:list` | admin/channel |
| GET | `/admin-api/v1/order-scenes/{orderCode}` | 场景订单详情 | `order:scene:query` | admin/channel |
| POST | `/agent-api/v1/order-scenes` | 代理人预约场景 | 登录即可 | agent |
| POST | `/client-api/v1/order-scenes` | 客户预约场景 | 登录即可 | client |
| PUT | `/admin-api/v1/order-scenes/{orderCode}/confirm` | 确认预约 | `order:scene:confirm` | admin/channel |
| PUT | `/admin-api/v1/order-scenes/{orderCode}/cancel` | 取消预约 | `order:scene:cancel` | admin/channel |
| GET | `/admin-api/v1/order-courses` | 课程订单分页列表 | `order:course:list` | admin |
| POST | `/client-api/v1/order-courses` | 客户购买课程 | 登录即可 | client |
| POST | `/agent-api/v1/order-courses` | 代理人代购课程 | 登录即可 | agent |
| GET | `/admin-api/v1/order-sojourns` | 旅居订单分页列表 | `order:sojourn:list` | admin |
| POST | `/client-api/v1/order-sojourns` | 客户预订旅居 | 登录即可 | client |
| POST | `/agent-api/v1/order-sojourns` | 代理人代订旅居 | 登录即可 | agent |
> 注：支付/退款接口已迁入结算域（见 §十七 finance-payments / finance-refunds），统一资金往来管理；订单状态日志已迁入系统域（system_order_status_log）。

---

## 十七、结算域接口（finance_）— 7表

| 方法 | 路径 | 说明 | 权限标识 | 端 |
|------|------|------|---------|-----|
| GET | `/admin-api/v1/finance-payments` | 支付记录列表 | `finance:payment:list` | admin/channel |
| POST | `/channel-api/v1/finance-payments` | 创建支付单 | 登录即可 | channel |
| POST | `/open-api/v1/finance-payments/notify` | 支付回调通知（第三方支付网关回调，签名验证） | 签名验证 | open |
| GET | `/admin-api/v1/finance-refunds` | 退款记录列表 | `finance:refund:list` | admin |
| POST | `/admin-api/v1/finance-refunds` | 发起退款 | `finance:refund:create` | admin |
| PUT | `/admin-api/v1/finance-refunds/{refundCode}/audit` | 审核退款 | `finance:refund:audit` | admin |
| GET | `/admin-api/v1/finance-flows` | 流水分页列表 | `finance:flow:list` | admin |
| GET | `/admin-api/v1/finance-flows/{flowCode}` | 流水详情 | `finance:flow:query` | admin |
| GET | `/admin-api/v1/finance-bills` | 结算单分页列表 | `finance:bill:list` | admin |
| GET | `/admin-api/v1/finance-bills/{billCode}` | 结算单详情 | `finance:bill:query` | admin |
| POST | `/admin-api/v1/finance-bills` | 创建结算单 | `finance:bill:create` | admin |
| PUT | `/admin-api/v1/finance-bills/{billCode}/audit` | 审核结算单 | `finance:bill:audit` | admin |
| PUT | `/admin-api/v1/finance-bills/{billCode}/settle` | 执行结算 | `finance:bill:settle` | admin |
| GET | `/admin-api/v1/finance-invoices` | 发票分页列表 | `finance:invoice:list` | admin |
| GET | `/admin-api/v1/finance-invoices/{invoiceCode}` | 发票详情 | `finance:invoice:query` | admin |
| POST | `/admin-api/v1/finance-invoices` | 创建发票 | `finance:invoice:create` | admin |
| PUT | `/admin-api/v1/finance-invoices/{invoiceCode}/issue` | 开票 | `finance:invoice:issue` | admin |
| PUT | `/admin-api/v1/finance-invoices/{invoiceCode}/send` | 寄出发票 | `finance:invoice:send` | admin |
| POST | `/channel-api/v1/finance-invoices/apply` | 渠道申请发票 | 登录即可 | channel |
| GET | `/admin-api/v1/finance-accounts` | 应收应付分页列表 | `finance:account:list` | admin |
| GET | `/admin-api/v1/finance-accounts/{accountCode}` | 账目详情 | `finance:account:query` | admin |
| PUT | `/admin-api/v1/finance-accounts/{accountCode}/receive` | 确认收款 | `finance:account:receive` | admin |
| GET | `/admin-api/v1/finance-reconciliations` | 对账记录分页列表 | `finance:recon:list` | admin |
| POST | `/admin-api/v1/finance-reconciliations` | 发起对账 | `finance:recon:create` | admin |
| PUT | `/admin-api/v1/finance-reconciliations/{reconCode}/confirm` | 确认对账 | `finance:recon:confirm` | admin |

---

## 十八、分销商域接口（distributor_）— 1表

> 管理接口走 `/admin-api/`（admin 代管）；分销商自有端走 `/distributor-api/`（与 `/supplier-api/` 对称，当前无 account 表，以下接口为预留）。

| 方法 | 路径 | 说明 | 权限标识 | 端 |
|------|------|------|---------|-----|
| GET | `/admin-api/v1/distributor-infos` | 分销商分页列表 | `distributor:info:list` | admin |
| GET | `/admin-api/v1/distributor-infos/{distributorCode}` | 分销商详情 | `distributor:info:query` | admin |
| POST | `/admin-api/v1/distributor-infos` | 新增分销商 | `distributor:info:create` | admin |
| PUT | `/admin-api/v1/distributor-infos/{distributorCode}` | 修改分销商 | `distributor:info:update` | admin |
| DELETE | `/admin-api/v1/distributor-infos/{distributorCode}` | 删除分销商 | `distributor:info:delete` | admin |
| PUT | `/admin-api/v1/distributor-infos/{distributorCode}/audit` | 审核分销商 | `distributor:info:audit` | admin |
| GET | `/admin-api/v1/distributor-infos/{distributorCode}/channels` | 分销商关联渠道列表 | `distributor:info:query` | admin |
| POST | `/distributor-api/v1/auth/login` | 分销商登录（预留） | 无需认证 | distributor |
| POST | `/distributor-api/v1/auth/logout` | 分销商退出（预留） | 登录即可 | distributor |
| GET | `/distributor-api/v1/distributor-infos/me` | 分销商自身信息（预留） | `distributor:self:query` | distributor |

---

## 十九、开放平台接口（open-api）

> 面向渠道系统对接的开放接口，使用 `app_key` + `app_secret` 签名认证。

### 19.1 认证机制

| 请求头 | 说明 |
|--------|------|
| `X-App-Key` | 渠道应用Key |
| `X-Timestamp` | 请求时间戳（秒） |
| `X-Nonce` | 随机字符串 |
| `X-Sign` | 签名值 = HMAC-SHA256(app_secret, app_key + timestamp + nonce + body) |

### 19.2 开放接口清单

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/open-api/v1/token` | 获取访问Token |
| GET | `/open-api/v1/contents` | 获取内容列表（按渠道配置过滤） |
| GET | `/open-api/v1/contents/{contentCode}` | 获取内容详情 |
| GET | `/open-api/v1/scenes` | 获取场景列表（按渠道配置过滤） |
| GET | `/open-api/v1/scenes/{sceneCode}` | 获取场景详情 |
| GET | `/open-api/v1/scenes/{sceneCode}/schedules` | 获取场景排期 |
| GET | `/open-api/v1/parks` | 获取养老机构列表 |
| GET | `/open-api/v1/parks/{parkCode}` | 获取机构详情 |
| GET | `/open-api/v1/equities` | 获取权益数据（按渠道） |
| POST | `/open-api/v1/equities/{equityCode}/activate` | 权益激活回调 |
| GET | `/open-api/v1/orders/{orderCode}` | 获取订单详情 |
| POST | `/open-api/v1/webhooks/register` | 注册Webhook回调地址 |
| GET | `/open-api/v1/webhooks/logs` | Webhook推送日志 |

---

## 二十、公共接口

### 20.1 文件上传

| 方法 | 路径 | 说明 | 端 |
|------|------|------|-----|
| POST | `/admin-api/v1/files/upload` | 通用文件上传 | admin |
| POST | `/channel-api/v1/files/upload` | 通用文件上传 | channel |
| POST | `/agent-api/v1/files/upload` | 通用文件上传 | agent |
| POST | `/client-api/v1/files/upload` | 通用文件上传 | client |

### 20.2 短信验证码

| 方法 | 路径 | 说明 | 端 |
|------|------|------|-----|
| POST | `/admin-api/v1/sms/send-code` | 发送验证码 | admin |
| POST | `/client-api/v1/sms/send-code` | 发送验证码 | client |
| POST | `/agent-api/v1/sms/send-code` | 发送验证码 | agent |

### 20.3 数据看板

| 方法 | 路径 | 说明 | 权限标识 | 端 |
|------|------|------|---------|-----|
| GET | `/admin-api/v1/dashboard/overview` | 工作台数据总览 | 登录即可 | admin |
| GET | `/channel-api/v1/dashboard/overview` | 渠道数据概览 | 登录即可 | channel |
| GET | `/agent-api/v1/dashboard/today` | 今日数据 | 登录即可 | agent |
| GET | `/admin-api/v1/reports/overview` | 数据总览报表 | `report:overview` | admin |

---

## 附录：完整接口清单汇总

| 序号 | 方法 | 路径 | 说明 | 权限标识 | 端 | 所属域 |
|------|------|------|------|---------|-----|--------|
| 1 | GET | /admin-api/v1/dict-commons | 字典分页列表 | system:dict:list | admin | 系统域 |
| 2 | GET | /admin-api/v1/dict-commons/tree | 字典树形 | system:dict:list | admin | 系统域 |
| 3 | GET | /admin-api/v1/dict-commons/{dictCode} | 字典详情 | system:dict:query | admin | 系统域 |
| 4 | POST | /admin-api/v1/dict-commons | 新增字典 | system:dict:create | admin | 系统域 |
| 5 | PUT | /admin-api/v1/dict-commons/{dictCode} | 修改字典 | system:dict:update | admin | 系统域 |
| 6 | DELETE | /admin-api/v1/dict-commons/{dictCode} | 删除字典 | system:dict:delete | admin | 系统域 |
| — | — | — | *（完整清单详见上述各业务域章节，此处不再逐条重复）* | — | — | — |

> **统计**：本文档共覆盖 17 个业务域，约 **480+** 个 API 接口（含 admin/channel/supplier/distributor/agent/client/open 七端）；精确清单以各域章节为准。

---

> **文档版本**: v1.1 | **最后更新**: 2026-08-03

---

> 本内容由 Coze AI 生成，请遵循相关法律法规及《人工智能生成合成内容标识办法》使用与传播。

> 2026-08-04 跨文档一致性修订：system/client/service 三域表计数对齐 v4.2 迁移（18/7/7）
