# 机构管理优化设计：素材库收口 / 富文本 / 业态板块 / 价格配置 / 信任链补全

- 日期：2026-08-14
- 状态：待用户审查
- 范围：dayan-admin（管理后台）、dayan-server（后端 19 模块 + dayan-job）、dayan-client（C 端）、dayan-agent（顾问端）

## 0. 背景与决策记录

本轮优化来自 5 条管理诉求，经代码库勘察（4 路探索）与设计讨论，用户已确认全部决策点：

| # | 诉求 | 决策 |
|---|------|------|
| 1 | 素材库承载本机构所有文件、视频、图片、VR | 服务端事务内统一登记；`park_code` 为空表示平台级素材 |
| 2 | 各模块可从素材库选取，也可独立上传（自动同步素材库） | 上传接口带素材上下文，同事务幂等登记，替代现有前端尽力注册 |
| 3 | 富文本字段用富文本编辑器编辑 | wangEditor v5 系（wangeditor-next 优先），服务端补 jsoup 净化 |
| 4 | 展示板块区分旅居/活力长居/照护长居，同一板块可属多业态 | `park_display_block.network_tags` 逗号分隔字段，空=全部业态 |
| 5 | 服务配置 → 价格配置 | 仅改 tab 文案，不拆分结构 |
| 6 | 补充项（P1） | 订单取价信任链、定价版本化与到点生效、C 端内容接口，全部纳入 |

工作流划分：**P0 = A 素材库、B 富文本、C 板块业态、D 改名；P1 = E 订单取价、F 定价版本化、G C 端内容接口。**

关键架构事实（设计依据）：
- 部署形态为**模块化单体**：`dayan-starters/dayan-admin` 聚合全部 19 个业务模块于同一 JVM（`dayan-server/dayan-starters/dayan-admin/pom.xml`），Spring 事件可跨模块同事务传播。
- `dayan-job` 通过直接依赖业务模块 + `@Scheduled` 调度器工作（如 `EquityExpireScheduler`），新增调度器沿用此模式。
- dayan-module-park 仅依赖 dayan-common-*，不依赖 dayan-module-system，跨模块登记必须解耦。
- agent 端 `rich-text :nodes` 直接渲染 HTML 字符串、**不做任何 URL 改写**（`dayan-agent/src/pages/business/park/vital/detail.vue:295`）。
- 最新迁移序号 44（`db/migration/44_drop_content_category_admin.sql`），本轮从 45 起。

---

## A. 素材库统一收口（P0）

### A.1 目标

- 机构管理过程中产生的**所有**上传（房型图、餐饮图、板块配图、顾问头像等）与平台级运营素材（文章封面、课程视频、商品图）都进入统一素材库。
- 「独立上传自动同步素材库」由**服务端同事务保证**，替代现状「前端组件异步调用 register 接口、失败仅 console.warn」（`dayan-admin/src/components/FileUploader/index.vue:99-114`）。
- 素材库获得全局入口与基础可用性（名称搜索、来源反查、删除保护）。

### A.2 服务端登记事件链（核心）

现状问题：登记由前端发起，仅 park 域 8 处调用点生效，goods/scene/course/content 等 30+ 处上传不入库；`db/archive/sync_assets.sql` 手工补数即为后果。

设计：

1. **上传接口扩展素材上下文参数**（`dayan-module-system/.../FileAdminController.java` 的 `POST /admin-api/v1/files/upload`）：
   - 新增可选参数：`assetRegister`（默认 false）、`assetParkCode`、`assetSourceType`、`assetSourceRef`。
   - 上传成功后（返回 DTO 前）发布 Spring 事件 `FileUploadedEvent`，携带：key、originalName、size、contentType、module、assetRegister、assetParkCode、assetSourceType、assetSourceRef、上传人 ID。
2. **事件类放 `dayan-common-core`**（`com.dayan.common.core.event.FileUploadedEvent`），保持 system 与 park 双向零编译依赖。
3. **park 模块新增监听器** `ParkAssetRegisterListener`（dayan-module-park）：`@EventListener` **同步**监听（同事务参与），`assetRegister=true` 时调用现有 `ParkAssetService.registerIfAbsent`（幂等键：parkCode + assetUrl + sourceType + sourceRefCode，保留现有语义）。登记失败抛异常回滚上传事务——OSS 对象可能残留为孤儿，与现状一致，可接受（对象只增不减是既定策略）。
4. **前端 `FileUploader` 改造**：
   - props：`assetParkCode/assetSourceType/assetSourceRef` 保留，新增 `registerAsset?: boolean`。
   - 上传时将素材上下文一并提交（`src/api/file.ts` 的 `uploadFile` 扩展 FormData 字段），**删除**前端 `registerAsset` 调用链（`src/api/park-asset.ts` 的 registerAsset 函数删除）。
   - 调用点改造：park 域 8 处（AssetPane/BasicTab/RoomTab/FoodTab/FacilityTab/AdviserTab/DisplayPane/ServiceItemPane）改传 `registerAsset`；goods/scene/course/content 封面等运营上传传 `registerAsset` 且不传 parkCode（→平台素材）；财务发票、头像等系统文件不传（不入素材库）。
5. **平台素材语义**：`park_asset.park_code = NULL` 表示平台级素材。`registerIfAbsent` 幂等查询对 null parkCode 用 `isNull`（MyBatis-Plus `eq(null)` 生成错误 SQL，需分支处理）。

### A.3 数据与接口变更

- `park_asset.park_code` 改为 `NULL` 允许，注释「归属机构编码，NULL=平台素材」（存量数据全部有值，语义不变）。
- 新增索引 `idx_asset_url(asset_url)`（幂等查询与引用校验加速）。
- `ParkAssetQueryDTO` 新增 `keyword`（模糊匹配 asset_name 或 asset_url，`wrapper.and(w -> w.like(...).or().like(...))`）；`buildWrapper` 同步（`ParkAssetServiceImpl.java:171-194`）。
- 列表/详情 VO 增加「来源」展示（sourceType/sourceRefCode 已有字段，前端补展示）。

### A.4 全局素材库页面

- 新页面 `dayan-admin/src/views/resource/asset/index.vue`：菜单挂在「资源管理」下（menu seed 追加，path `/resource/asset`，复用已有权限组 `park:asset:list/query/create/update/delete`，见 `db/migration/seed/rbac_permission_seed.sql:91-96`）。
- 功能：筛选（归属机构下拉含「平台素材」/类型/业务分类/来源/关键字/状态）+ 分页网格 + 预览（图/视频/PDF）+ 元数据编辑 + 删除（走 A.5 校验）+ VR 子 tab。
- 机构详情内 `AssetTab` 保留，作为按 parkCode 预过滤的视图复用同一 `AssetPane`。
- 「从素材库选择」：FileUploader 增加「从素材库选择」入口（弹窗选素材回填 key）——机构域与富文本插图共用。一期实现图片/视频类型选取。

### A.5 删除保护（引用校验）

- `ParkAssetServiceImpl.delete`（现直接 deleteById）前置引用检查：配置化声明「引用地图」`AssetRefMap`（常量类，表 → 候选列清单），覆盖：
  - 结构化列精确匹配：`park_info`（logo/head_images 等）、`park_room_type`、`park_food_type`、`park_facility_type`、`park_adviser.avatar`、`goods_info`（cover_image/video_url；image_urls 为 JSON 数组用 `LIKE '%"key"%'`）、`scene_info`、`course_info`、`content_info.cover_image`、`content_media.asset_url`、`park_asset`（cover_url/thumbnail_url 自引用）。
  - HTML 字段兜底 `LIKE '%key%'`：`park_display_block.content`、`content_info.content_body`。
- 任一命中 → 抛业务异常（返回引用表清单），拒绝删除；零引用 → 逻辑删除（现状语义）。OSS 对象一律不物理删除。
- 引用地图以常量类维护（表量小、每表一次 count，不做动态 SQL 框架）。

### A.6 VR 元数据补齐

- 上传后缀白名单（`FileAdminController.ALLOWED_EXT`）追加：`zip`、`obj`、`glb`、`gltf`（3D/全景包），并保持既有图片类型（2:1 全景图走普通图片上传，assetCategory 已有「全景VR」分类）。
- `vr_provider` 字典化：`system_dict_business` 种子 `dict_type='vr_provider'`（如 krpano/threejs/720yun/自研），前端下拉替换自由文本。
- VR 播放器集成**不在本轮**（见非目标）。

### A.7 错误处理

- 登记失败 → 回滚上传（用户感知为上传失败并重试），杜绝「上传成功但素材库缺失」。
- 引用校验命中 → 明确报错「素材被 X 处引用，无法删除」，引导先替换引用。
- `keyword` 搜索对 NULL asset_name 行为：LIKE 自动跳过 NULL，无特殊处理。

---

## B. 富文本编辑（P0）

### B.1 编辑器选型

首选 **wangeditor-next**（`@wangeditor-next/editor` + `@wangeditor-next/editor-for-vue`，社区维护 fork，原 wangEditor 5 已停更）；备选原版 `@wangeditor/editor@^5` + `@wangeditor/editor-for-vue@next`。以实现时依赖解析可用性为准，二者 API 一致。

理由：Vue3 官方适配、中文文档、自定义上传函数（customUpload）可直连现有上传链路、输出标准 HTML 与小程序 `rich-text` 白名单兼容。

### B.2 RichEditor 组件

新建 `dayan-admin/src/components/RichEditor/index.vue`：

- Props：`modelValue`（HTML string）、`placeholder`、`height`（默认 400px）、素材上下文透传（`module/assetParkCode/assetSourceType/assetSourceRef`，转发给上传接口 → 走 A.2 登记链）。
- 工具栏：标题/加粗斜体/列表/引用/链接/图片/视频/表格/撤销重做；字体颜色等按需精简。
- 图片/视频 `customUpload` → `uploadFile(file, 'rich', 素材上下文)` → 以返回的 `absoluteUrl` 插入（见 B.4）。
- 存量手写 HTML 直接回显（字段与表结构不变，天然兼容）。

### B.3 接入点（三处）

| 位置 | 字段 | 现状 |
|------|------|------|
| `resource/content/detail/BasicTab.vue:158-160` | `contentBody` 文章正文 | textarea「支持 HTML」 |
| `resource/park/detail/DisplayPane.vue:317-325` | `content` 板块正文 | textarea「支持 HTML 富文本」 |
| `resource/park/detail/BasicTab.vue:670` | `baseDescription` 基地简介 | 纯文本编辑、agent 端按 HTML 渲染（口径不一致） |

三处统一替换为 RichEditor；`baseDescription` 由此统一编辑/渲染口径。

### B.4 内嵌资源 URL 规范

约束：agent 端 `rich-text` 直接渲染 HTML、零改写；C 端同理。因此**富文本内嵌资源必须存完整 URL**（与业务字段存裸 key 的惯例不同）。

设计：
- `StorageProperties` 新增 `dayan.storage.public-base-url`；`absoluteUrl` 生成规则：配置了该值 → `{public-base-url}/{key}`，未配置 → 回退 MinIO 公开桶直链 `{endpoint}/{bucket}/{key}`（默认桶 `dayan-public` 即公开桶）。
- `FileUploadDTO` 新增 `absoluteUrl` 字段，上传响应由服务端拼好完整 URL；RichEditor 插图只使用 `absoluteUrl`。
- 存量 HTML 中的相对/手写 URL 不做迁移（`formatFileUrl` 已兼容 http 开头原样返回）。

### B.5 服务端净化（XSS）

现状 `contentBody`/`content`/`baseDescription` 裸 String 入库（无 jsoup/sanitizer，hutool 未用于此），分发至 agent/C 端渲染，属实际风险。

- `dayan-common-core` 新增 `HtmlSanitizer`（引入 jsoup 1.17.x，依赖加在 common-core pom）。
- Safelist 自定义（`Safelist.relaxed()` 为底）：允许 `img[src|alt|width|height|style]`、`video[src|poster|controls|width|height]`、`source`、`a[href|title|target]`、`p/br/h1-h6/ul/ol/li/blockquote/strong/em/u/s/span[style]/table/thead/tbody/tr/td/th`；强制 `rel=noopener`、`target=_blank`；禁止 `script/iframe/object/embed`、一切 `on*` 事件属性、`javascript:` 协议。
- 应用点：`ContentInfoService`（contentBody）、`ParkDisplayBlockService`（content）、`ParkInfoService`（baseDescription/specialtyDescription）保存链路统一 `clean()`。净化失败（解析异常）抛参数错误。

### B.6 错误处理

- 编辑器上传失败复用现有 request 拦截器 toast；正文已编辑内容不丢失（组件本地态）。
- 净化后 HTML 与用户提交不一致属预期（静默修正），日志记录 diff 长度。

---

## C. 展示板块 × 业态（P0）

### C.1 数据模型

- `park_display_block` 新增 `network_tags VARCHAR(64) NULL`，注释「适用业态（逗号分隔：vital/care/sojourn），空=全部业态」。存量数据 NULL → 三业态全展示，**零迁移兼容**，C 端行为与今天一致。
- 多对多用逗号分隔存储：每机构板块量级十几条、接口按 parkCode 全量取后过滤，无独立查询需求，不建中间表。

### C.2 业态统一枚举与字典

- `dayan-common-core` 新增枚举 `NetworkType { VITAL("活力长居"), CARE("照护长居"), SOJOURN("旅居") }` + 解析/校验工具（逗号串 ↔ 枚举集）。
- `system_dict_business` 种子 `dict_type='network_type'`（vital/care/sojourn），供前端下拉复用。
- 现存三套业态表示（`park_info.network_tags` 字符串、`service_item.item_subtype` 数字、`agent_lead.interest_type` 自由文本）中，仅 `network_tags` 体系接入枚举校验；另两处标记 `@Deprecated` 注释，不做数据迁移。

### C.3 管理端

- `DisplayPane.vue` 板块表单新增「适用业态」`el-checkbox-group`（活力长居/照护长居/旅居，默认不勾选=全部）；列表列展示业态标签；编辑回显。
- `ParkDisplayBlockCreate/UpdateDTO` 新增 `networkTags`，服务端校验：逗号分隔、去重、值 ∈ 枚举。

### C.4 出口过滤

- `ParkFullDetailVO` 的 displayBlock VO 新增 `networkTags` 字段。
- **client 端**：`GET /client-api/park/{parkCode}/full` 新增可选 `network` 参数——有值时服务端过滤（`network_tags` 含该业态或为空），返回已过滤板块。
- **agent 端**：`/agent-api/park/{code}/full` 返回全量 + `networkTags` 字段，三个业态详情页（`dayan-agent/src/pages/business/park/{vital,care,sojourn}/detail.vue`）按自身业态在端上过滤（页面已有业态上下文，改动局部）。
- dayan-client 三业态详情页改传 `network` 参数。

---

## D. 服务配置 → 价格配置（P0）

仅改文案：`dayan-admin/src/views/resource/park/detail/index.vue:62-70` 的 tab label「服务配置」→「价格配置」。不拆分 ServiceConfigTab 结构、不动接口与权限。

---

## E. 订单取价信任链（P1）

### E.1 问题

权益订单创建时服务端以 `goods.getSalePrice()` 覆盖客户端价格（`AgentOrderEquityController.java:78-84`），但：
- 旅居订单四项费用（room/care/food/other）与押金**直接采信客户端 DTO**（`OrderSojournServiceImpl.java:104-118`），不回查 `park_pricing` 或 `goods_sku_sojourn`。
- 场景订单 `unitPrice` 直接采信 DTO（`OrderSceneServiceImpl.java:106-123`）。

存在篡改与口径不一致风险（资金安全）。

### E.2 设计

- **服务端权威取价**：
  - 旅居订单：组价顺序 = `goods_sku_sojourn`（park + room_type + care_type + food_type 组合精确匹配，SKU 自带 `sku_price`）→ 逐项 `park_pricing`（`is_current=1`，charge_type 1/2/3 对应房/照护/餐）→ 仍无则抛「该机构未配置价格，请先在价格配置中维护」。
  - 场景订单：`scene_item_price`（按订单渠道 channelCode 取 `channel_price`）→ `goods_info.sale_price` → 仍无则抛错。
- **金额一律以服务端计算为准入账**（`pay_amount` 及各项费用快照）；客户端传值仅作比对日志。
- **偏差处理模式**（配置 `dayan.order.price-check`，默认 `strict`）：
  - `strict`：客户端值与服务端价偏差 > 1 元或 > 1% → 拒单，提示联系运营核对；
  - `warn`：偏差记 warn 日志，仍按服务端价入账；
  - `off`：维持现状（仅留作回滚开关）。
- 优惠（discount_amount）保留现有算术与合理性校验。
- 权益订单链路已合规，不动。

### E.3 影响面

渠道端/代理端「面议价」「定制折扣」类历史操作将被告警拦截——属预期收紧；正规定制折扣应通过定价/商品价格配置或后续优惠能力承接。上线顺序建议先 `warn` 观察日志一周，再切 `strict`（运维操作，不改代码）。

---

## F. 定价版本化与到点生效（P1）

### F.1 问题

`ParkPricingServiceImpl.update` 直接修改原价格记录，不留版本；`effective_date` 存在但无任务在到期日自动翻转 `is_current`，当前价靠人工维护。

### F.2 调价语义（新增 revise）

- 新接口 `POST /admin-api/park/pricing/{id}/revise`，参数：`originalPrice/salePrice/discountRate`（至少一项）、`effectiveDate`（可等于现在=立即生效）、`priceChangeReason`。
- 「同维度」= `park_code + charge_type + ref_code + billing_cycle`（即现有 `uk_current` 唯一索引维度）。
- 行为（同事务）：
  - **立即生效**：旧记录 `is_current=0` → 插入新记录 `is_current=1`（先置旧再插新，配合 `uk_current` 唯一索引防并发双当前价）。
  - **预约生效**：插入新记录 `is_current=0, pending_flag=1, effective_date=未来`；若同维度已存在未生效的 pending 记录 → 旧 pending 逻辑删除（被顶替），保证一条链清晰。
- `update` 接口收窄：价格数值字段（originalPrice/salePrice/discountRate）从 UpdateDTO 移除，仅允许改 planName/includesItems/promotion 描述等非价格字段——防止绕过版本化直改历史价格。
- 历史价即 `is_current=0` 的记录，前端提供「版本历史」查看。

### F.3 到点切换调度器

- `park_pricing` 新增列 `pending_flag TINYINT(1) DEFAULT 0`（注释「预约生效标记：1=待生效，0=无/已生效」）。
- `dayan-job` 新增 `PricingEffectiveScheduler`（pom 增加依赖 dayan-module-park，模式照抄 `EquityExpireScheduler`）：每小时 :05 扫描 `pending_flag=1 AND effective_date <= now`，逐条同事务执行「旧当前置 0 → 本条置 1 且 pending_flag=0」；单条失败记日志不中断，分批处理。
- `expire_date` 到期**不自动失效**（避免无价可用），仅日志告警，由运营决定是否 revise。

### F.4 管理端

- 价格配置各子面板的价格行：新增「调价」按钮（弹窗：新价/生效日期含「立即生效」快捷/原因）与「版本历史」入口（同维度全部版本列表，标注当前价）。

---

## G. C 端内容接口（P1）

### G.1 问题

content 模块 `controller/client` 为空占位；`dayan-client/src/api/home.ts` 的 `/banners`、`/recommend` 无后端支撑（try/catch 降级）。三业态 C 端页面无法展示内容流。

### G.2 数据模型

- `content_info` 新增 `network_tags VARCHAR(64) NULL`（语义同板块：空=全部业态），存量数据零迁移兼容。
- admin `content/detail/BasicTab.vue` 新增「适用业态」多选（复用 C.3 同款交互与枚举校验）；列表可选展示业态列。

### G.3 接口（dayan-module-content `controller/client/ClientContentController`）

| 接口 | 说明 |
|------|------|
| `GET /client-api/contents?network=&categoryCode=&page=&size=` | 已发布内容列表；network 过滤（tags 含该业态或为空） |
| `GET /client-api/contents/{contentCode}` | 详情（仅已发布；阅读量 +1，异步累加） |
| `GET /client-api/contents/banners?network=` | 轮播位：已发布 + is_top/is_recommend，返回封面与跳转信息，条数上限 8 |
| `GET /client-api/contents/recommend?network=&limit=` | 推荐内容：is_recommend=1 已发布 |

- 鉴权口径与 `ParkClientController` 一致（公开只读）。
- 返回 VO 对齐 `dayan-client/src/types` 的 Banner/内容类型（实现时以客户端类型定义为准核对字段）。

### G.4 客户端对接

- `dayan-client/src/api/home.ts` 改调 `/contents/banners`、`/contents/recommend`（保留 try/catch 降级逻辑）；三业态首页/内容位按 `network` 参数取数。

---

## 数据库迁移清单（db/migration，从 45 起）

| 序号 | 文件 | 内容 |
|------|------|------|
| 45 | `45_asset_hub.sql` | `park_asset.park_code` NULL 化（含注释）；`idx_asset_url`；素材库菜单 seed；`vr_provider` 字典 seed |
| 46 | `46_network_type.sql` | `network_type` 字典 seed |
| 47 | `47_display_block_network.sql` | `park_display_block.network_tags` |
| 48 | `48_pricing_revision.sql` | `park_pricing.pending_flag` |
| 49 | `49_content_network.sql` | `content_info.network_tags` |

幂等要求：延续既有迁移风格（IF NOT EXISTS / 存在性判断），支持重复执行；seed 与 `menu_seed.sql`/`rbac_permission_seed.sql` 风格一致。

> 序号说明：表中编号为规划占位。因计划一按 D→C→A→B 交付、计划二按 E→F→G 交付，**实际迁移文件按落地顺序从 45 起顺延编号**，内容与上表一一对应。

## 兼容性设计

- 存量 `park_asset` 全部有 parkCode，语义不变；平台素材是纯新增能力。
- 板块/内容 `network_tags` NULL=全业态，C 端与 agent 端行为不变。
- 富文本字段存量手写 HTML 编辑器直接回显；表结构不变。
- `formatFileUrl` 兼容 http 完整 URL（历史手填）与裸 key 双格式，不受 B.4 影响。
- `park/pricing` update 行为收窄（价格字段不可直改）仅影响 admin 前端，同步改造，无第三方调用方。
- 订单 strict 模式可通过配置降级 `warn`/`off`，留回滚开关。

## 测试策略

服务端单测（延续现有 JUnit5 模式）：
- `ParkAssetRegisterListener`：登记幂等（含 parkCode=NULL 分支）、assetRegister=false 不登记、失败回滚。
- 删除保护：各引用形态（精确列/JSON LIKE/HTML LIKE）命中与放行。
- `HtmlSanitizer`：XSS 向量表（script/on*/javascript: 协议/iframe）剥离、合法富文本标签保留。
- `NetworkType` 校验与板块/内容 network 过滤（含 NULL=全部）。
- pricing revise：立即生效/预约生效/pending 顶替/uk_current 并发保护；调度器到点切换。
- 订单取价：strict 三种偏差行为、无价报错、取价顺序（SKU→pricing；channel_price→salePrice）。

前端：dayan-admin/dayan-client 无单测基建，以构建 + ESLint 通过为准，辅以手工验收清单（每工作流 3-5 条：素材上传即入库、板块业态过滤三端表现、富文本插图绝对 URL 可在 agent 端渲染、调价历史版本、C 端 banners 真实返回等）。

回归重点：agent 端存量 HTML 渲染（净化后不破版）；订单创建链路（warn 模式灰度）。

## 非目标（明确排除）

- VR 播放器/全景渲染集成（仅补上传格式与元数据字典）。
- OSS 孤儿对象自动清理与对账任务。
- `service_item.item_subtype`、`agent_lead.interest_type` 向 NetworkType 的数据迁移。
- 床位级管理、客户入住合同域、渠道差异化定价、素材标签体系、引用计数前端展示。
- 「价格配置」tab 结构拆分（用户决策：仅改名）。

## 实施拆分建议

规模横跨 7 个工作流，建议**两个实现计划**分批交付：
- **计划一（P0）**：D（改名，先行独立提交）→ C（板块业态）→ A（素材库收口）→ B（富文本，依赖 A 的上传上下文链路）。
- **计划二（P1）**：E（订单取价）→ F（定价版本化）→ G（C 端内容接口）。

每计划独立分支、独立验收；迁移 45-49 随所属工作流提交。
