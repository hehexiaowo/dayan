# P9 计划：Admin 端扩展页补全（主从详情页 / tab 式）

> 阶段定位：后端 100% 完成、5/5 E2E PASS 之后的 Admin 前端增量。
> 组织决策（用户确认）：**主从详情页 tab 式**——以主实体（机构/供应商）详情页为核心，子表以 tab 内联管理，而非割裂的独立列表页。
> supplier RBAC 三件套（role/permission/openplatform）：**跳过**，TC-E2E-005 已裁定供应商走 Admin 代录。

## 1. 背景与问题重构

### 1.1 机械方案的缺陷
后端 park 模块 15 个 controller、supplier 8 个 controller。若"一个 controller 一个独立列表页"，会产生：
- park 14 个割裂列表页 + supplier 7 个割裂列表页
- 运营心智割裂：要管理"阳光颐养中心"这个机构，需在 14 个页面间跳转，每个都要重新输入/筛选 parkCode
- type+price 本是一体（录房型必带价格），拆成两个列表页后，E2E 报告记的"价格需先建 type 拿 code 再回填"痛点会原样复现

### 1.2 主从详情页方案
以主实体为锚点：
- **主列表页**（已有，如 `resource/park/index.vue`）：列表 + 操作列增加"详情/管理"入口
- **详情页**（新增，如 `resource/park/detail/[parkCode]/index.vue`）：顶部展示主实体摘要，下方 el-tabs 按子表维度分 tab，每个 tab 内是该子表的内联 CRUD（表格 + 弹窗/抽屉）
- tab 内的子表用 parkCode 自动过滤，无需运营重复输入

## 2. 工作量重构（机械 74 页 → 详情页 10 个 + tab 26 个）

### 2.1 park 机构详情页（1 个详情页 + 8 个 tab）
对应 14 个 controller，合并为 8 个 tab：

| Tab | 对应 controller | 形态 |
|-----|----------------|------|
| 基本信息 | ParkInfo（主表，已有编辑弹窗复用） | 主表字段编辑表单 |
| 房型 | ParkRoomType + ParkRoomPrice | type 列表，展开行内联 price 列表（解决 type+price 一体） |
| 照护 | ParkCareType + ParkCarePrice | 同房型模式 |
| 餐饮 | ParkFoodType + ParkFoodPrice | 同房型模式 |
| 媒体库 | ParkMediaImage + Video + File + Vr（4 个） | 统一列表，mediaType 筛选/tab 切分 |
| 设施 | ParkFacility | 简单列表 CRUD |
| 顾问 | ParkAdviser | 简单列表 CRUD |
| 周边 + 服务项 | ParkPeriphery + ParkServiceItem | 两块分区或合一个 tab |

### 2.2 supplier 供应商详情页（1 个详情页 + 4 个 tab）
对应 7 个 controller，跳过 RBAC 3 件套后剩 4 个子表：

| Tab | 对应 controller | 形态 |
|-----|----------------|------|
| 基本信息 | SupplierInfo（主表） | 主表字段编辑 |
| 合同 | SupplierContract | **独立列表页**（合同有 6 态状态机 + 到期管理，独立页更合适，详情页放入口链接） |
| 联系人 | SupplierContact | 简单列表 CRUD |
| 评价 | SupplierEvaluation | 列表 + 查看详情 |

> 合同的取舍：它有独立的状态机（草稿/待审/生效/到期/终止/作废）和到期管理价值，做成独立列表页（`resource/supplier/contract/index.vue`），从供应商详情页可跳转。其余子表进详情 tab。

### 2.3 其他业务域（按需，优先级低于 park/supplier）
其余模块（finance/butler/client/channel/agent/service/goods/scene/content/course/equity）的子表，逐模块评估"是否值得详情页 vs 独立列表页"，**留到 P9.2**。本计划（P9.1）只做 park + supplier 两个详情页 + supplier 合同独立页。

### 2.4 本批次（P9.1）实际产出
- park 详情页 1 个 + 8 tab 内联组件
- supplier 详情页 1 个 + 4 tab 内联组件
- supplier 合同独立列表页 1 个
- 前端静态路由机制（详情页带参路由，隐藏菜单）
- 复用的内联 CRUD 子组件（`InlineTable` + `useCrud` 适配 tab 场景）

## 3. 全局约束（写代码前必须遵守）

### 3.1 路由机制
- 动态路由（`router/dynamic.ts`）按后端菜单树生成，详情页不在菜单树里
- 详情页用**前端静态路由**（`router/index.ts` 里 `staticRoutes`），path 模式 `/resource/park/detail/:parkCode`，`meta: { hidden: true }` 不进侧边栏
- 主列表页操作列加"详情"按钮 → `router.push({ name: 'ParkDetail', params: { parkCode } })`

### 3.2 主键与 code 生成
- park 子表主键是 `id`（Long，AUTO_INCREMENT），**非 CodeGenerator code**——前端表单不含 id（创建时后端返回）
- park 子表的 `parkCode` 是外键关联，由详情页路由参数提供，子表 CRUD 自动携带
- type+price：price 的 `roomTypeCode` 等是外键，在 type 展开行内由当前 type 提供

### 3.3 类型对齐
- 后端部分 controller 返回 VO（如 `ParkRoomTypeVO`）而非裸 Entity——前端 types 必须对齐 VO 字段，不能照抄 Entity
- 每个 controller 的 CreateDTO 必填字段（@NotNull/@NotBlank）决定前端表单校验规则

### 3.4 tab 组件
- Element Plus `el-tabs` + `el-tab-pane`（项目当前无使用先例，本批次首次引入）
- type+price tab 用 `el-table` 的 `expand` 行展开内联 price 列表

## 4. 任务分解（SDD 子智能体驱动）

> 每个任务一个子智能体实现 + 一个子智能体审查，审查通过后进下一个。
> Base 分支：当前 main（00b62ea）。

### 任务 0：前端详情页路由机制 + 共享组件（基础设施）
- 在 `router/index.ts` 增加 staticRoutes 区段，支持 `hidden` meta
- 实现 `InlineTable` 内联表格组件（封装 el-table + 分页 + 弹窗编辑，适配 tab 内场景）
- 实现 `useCrud` 的 tab 适配：接受 `fixedParams`（如 parkCode）自动合并到查询
- 产出 park 详情页骨架（`views/resource/park/detail/[parkCode]/index.vue`：顶部摘要 + el-tabs 空壳）和 supplier 详情页骨架
- **验证**：vue-tsc 通过 + npm run build 通过 + 详情页路由可访问

### 任务 1：park 详情页 — 基本信息 + 房型 + 照护 + 餐饮 tab（核心业务）
- 4 个 tab 的 types/api/views 实现
- 房型/照护/餐饮：type 列表 + 展开行 price 列表的内联管理
- 调研后端 6 个 controller（RoomType/RoomPrice/CareType/CarePrice/FoodType/FoodPrice）的端点 + CreateDTO 必填字段
- 基本信息复用现有 ParkInfo 编辑表单逻辑
- **验证**：vue-tsc + build + 手动确认 tab 切换、type+price 内联 CRUD 交互

### 任务 2：park 详情页 — 媒体库 + 设施 + 顾问 + 周边/服务项 tab
- 4 个 tab：媒体库（4 controller 合并）、设施、顾问、周边+服务项
- 调研后端 8 个 controller 的端点 + DTO
- **验证**：vue-tsc + build + tab 交互

### 任务 3：supplier 详情页 + 合同独立页
- supplier 详情页 4 tab（基本信息/联系人/评价 + 合同入口）
- 合同独立列表页（6 态状态机 transition 按钮）
- 调研后端 SupplierContract/Contact/Evaluation 的端点 + DTO + 合同状态机
- **验证**：vue-tsc + build + 合同 transition 交互

### 任务 4：回归 + E2E 烟测
- vue-tsc + npm run build 全量
- 启动 admin 前端，对 park 详情页、supplier 详情页做 GUI 烟测（列表→详情→tab 切换→子表 CRUD）
- 更新 E2E 报告 / progress.md

## 5. 验收标准
- vue-tsc --noEmit EXIT=0
- npm run build EXIT=0
- park 详情页：从机构列表点"详情"进入，8 个 tab 均可切换，房型/照护/餐饮的 type+price 内联 CRUD 可用，媒体库 4 类可见
- supplier 详情页：4 tab 可用，合同独立页 transition 可触发

## 6. 不在本批次范围（P9.2 候选）
- 其他业务域（finance/butler/client/channel/agent/service/goods/scene/content/course/equity）的详情页或子表页
- supplier RBAC 三件套（永久跳过，除非启用 SUPPLIER 端）
- 小程序端端到端（独立 P10）
- 横向质量加固（测试/性能/安全，独立 P11）

## 7. 风险
- **VO 字段未确认**：任务 1/2/3 实现前必须先 Read 对应 VO/DTO 类，不能照抄 Entity（task-brief 会强制约束）
- **媒体 4 表字段差异**：虽然 controller 同构，image/video/file/vr 的字段可能不同（如视频有 duration、VR 有 panoramaUrl），媒体库 tab 要按 mediaType 条件渲染列
- **详情页路由权限**：hidden 路由不走菜单权限，但要确认路由守卫不会拦截（现有守卫只校验登录态，应无碍）
