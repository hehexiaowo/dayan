# P9+P10 设计规格：Channel 端 + Agent/Client 小程序（第 1 增量）

> 阶段：P9（Channel 渠道后台）+ P10（Agent 代理人端 + Client 客户端小程序）  
> 目录：`dayan-web-channel/`、`dayan-miniprogram-agent/`、`dayan-miniprogram-client/`  
> 参考：P8 Admin 前端已建立的模式（动态路由/useCrud/类型基类）、各端已有 P0 脚手架（登录可用）

---

## 0. 全局约束

### 0.1 三端现状（P0 脚手架已就绪）
- **Channel（dayan-web-channel）**：Vue3/TS/Element Plus/Pinia/Router/Axios，代理 `/channel-api`→8000。已有 login/dashboard/layout/404 + auth api + user store + request.ts（Channel-Token）。**与 P8 前 Admin 状态一致**（含 global.d.ts 隐患）。
- **Agent（dayan-miniprogram-agent）**：uni-app 3.0（@dcloudio）+ Vue3 + Pinia + uview-ui。已有 login（选渠道流程）+ 4 Tab（home/acquisition/customer/activity）占位页 + request.ts（uni.request 封装，Agent-Token）+ user store + pages.json（tabBar）。
- **Client（dayan-miniprogram-client）**：uni-app 同 Agent 栈。已有 login + 4 Tab（home/park/service/mine）占位页 + request.ts（Client-Token）+ user store + pages.json。

### 0.2 后端契约现状（关键约束）
- **各端仅 auth controller 就绪**：ChannelAuthController（/channel-api/auth）、AgentAuthController（/agent-api/auth，含选渠道 /channels）、ClientAuthController（/client-api/auth）。
- **业务端 controller 均未实现**（无 channel/agent/client scoped 业务接口）。
- **结论**：本期为"UI 框架 + 页面骨架 + API 契约层"。业务页面调用未实现的端点时，**统一降级处理**（try/catch + 空状态 + "接口待后端提供"提示），与 P8 操作日志页同策略。**不阻塞构建验收**。

### 0.3 API 路径契约
- Channel：`/channel-api/{resource}`（context-path=/channel-api，controller @RequestMapping("/xxx")）。
- Agent：`/agent-api/{resource}`，request.ts 已拼 BASE_URL='/agent-api'。
- Client：`/client-api/{resource}`。
- 响应拆包：R<T> code===0 取 data（各端 request 已处理）。

### 0.4 Channel 菜单数据
channel 域菜单（domain_type=channel）**未 seed**。本期 Channel 端采用**静态路由**（不依赖后端菜单树），在 router 里硬编码渠道业务页面路由。动态菜单留后续（需先 seed channel 菜单 + 补 channel 端 menu controller）。

---

## 1. P9：Channel 渠道后台（dayan-web-channel）

### 1.1 基础架构移植（从 P8 Admin 复用）
Channel 与 Admin 同栈，直接移植 P8 已验证的基础设施（适配 Channel-Token / channel-api）：
- **修复 global.d.ts → global.ts**（同 P8 fix，request.ts 导入运行时常量）。
- **修复 request.ts 类型断言**（`service.request(config) as Promise<T>`）。
- 移植 `types/common.ts`（PageResult/PageQuery/CommonStatus）。
- 移植 `composables/useCrud.ts`。
- **router 改静态业务路由**（不移植动态菜单，因 channel 菜单未 seed）：在 `/` Layout children 下注册渠道业务页面。
- layout 侧边栏改为**静态菜单渲染**（硬编码渠道业务菜单项，非递归组件）。

### 1.2 Channel 业务页面（5 页，静态路由）
Channel 是渠道运营后台，管理本渠道的代理人/客户/权益/订单。每页 = view + api + types，调用 /channel-api/xxx（后端待实现，降级处理）。

| 页面 | 路由 | 视图文件 | 后端预期端点（待实现） |
|------|------|---------|---------------------|
| 工作台 | /dashboard | views/dashboard/index.vue（增强：渠道概览卡片） | GET /channel-api/dashboard/stats |
| 代理人管理 | /agent | views/agent/index.vue | GET /channel-api/agents |
| 客户管理 | /client | views/client/index.vue | GET /channel-api/clients |
| 权益查询 | /equity | views/equity/index.vue | GET /channel-api/equities |
| 订单查询 | /order | views/order/index.vue | GET /channel-api/orders |

每页结构：搜索栏 + el-table（useCrud）+ 详情弹窗。onMounted try/catch 降级。

### 1.3 类型与 API
- types/agent.ts、client.ts、equity.ts、order.ts（字段参考各域 Entity，渠道视角子集）。
- api/agent.ts 等（/channel-api/xxx）。

---

## 2. P10-A：Agent 代理人小程序（dayan-miniprogram-agent）

### 2.1 现状
login（选渠道）+ 4 Tab 占位已就绪。填充 4 Tab 业务内容。

### 2.2 4 Tab 页面填充
| Tab | 文件 | 功能 |
|-----|------|------|
| 首页 home | pages/home/index.vue | 顶部欢迎卡（代理人姓名/渠道）+ 功能入口宫格（获客/客户/活动/业绩）+ 待办/通知列表 |
| 获客 acquisition | pages/acquisition/index.vue | 客户线索列表（card 列表，下拉刷新）+ 新增线索按钮（跳转或弹层）+ 分享获客码入口 |
| 客户 customer | pages/customer/index.vue | 我的客户列表（搜索 + card）+ 客户详情（绑定关系/健康档案简览） |
| 活动 activity | pages/activity/index.vue | 活动/内容素材列表（card 图文）+ 分享按钮 |

### 2.3 新增支撑文件
- api/agent.ts（代理人信息 /agent-api/agent/info）、api/customer.ts（客户列表 /agent-api/customers）、api/share.ts（分享/业绩 /agent-api/share）。
- types/ 对应类型。
- 各页 try/catch 降级（后端业务接口未实现）。

### 2.4 uni-app 规范
- 用 uni-app 组件（view/text/image/button/scroll-view）+ uni.* API（request/storage/showToast/navigateTo）。
- 不用 Element Plus（小程序不支持）；样式用 rpx 单位。
- 页面间跳转 uni.navigateTo/reLaunch；Tab 页 uni.switchTab。
- 已有 request.ts 封装，复用。

---

## 3. P10-B：Client 客户端小程序（dayan-miniprogram-client）

### 3.1 现状
login + 4 Tab（home/park/service/mine）占位。

### 3.2 4 Tab 页面填充
| Tab | 文件 | 功能 |
|-----|------|------|
| 首页 home | pages/home/index.vue | 顶部 banner + 功能宫格（找机构/服务/权益/订单）+ 推荐内容/机构 |
| 机构 park | pages/park/index.vue | 机构列表（搜索 + 筛选 + card 图文）+ 机构详情入口 |
| 服务 service | pages/service/index.vue | 我的服务会话列表（状态标签）+ 发起服务入口 |
| 我的 mine | pages/mine/index.vue | 个人信息卡 + 我的权益/订单/地址/设置入口列表 |

### 3.3 新增支撑文件
- api/park.ts（机构 /client-api/parks）、api/service.ts（服务 /client-api/services）、api/equity.ts（权益 /client-api/equities）、api/order.ts（订单 /client-api/orders）。
- types/ 对应类型。
- 各页 try/catch 降级。

---

## 4. 任务拆分

| 任务 | 内容 | 执行 |
|------|------|------|
| P9-spec | 本规格 | 完成 |
| P9 | Channel：基础架构移植（global.ts/request/useCrud/common）+ 静态路由/layout + 5 业务页 | 1 子智能体（标准） |
| P10-A | Agent 小程序 4 Tab 填充 + api/types | 1 子智能体（标准） |
| P10-B | Client 小程序 4 Tab 填充 + api/types | 1 子智能体（标准） |
| P9+P10-verify | Channel npm run build；Agent/Client uni build（或类型检查） | 控制者 |

**并行性**：P9（dayan-web-channel）、P10-A（dayan-miniprogram-agent）、P10-B（dayan-miniprogram-client）三个目录完全隔离，**可三路并行**。

---

## 5. 验收标准
1. Channel：`npm run build`（vue-tsc + vite）exit 0。
2. Agent/Client：`npm install` + `npm run build:h5`（uni build H5）成功，或至少 tsc 无类型错误（uni build 需完整依赖）。
3. 各端登录页保留可用（不破坏已有 auth 流程）。
4. 业务页 API 路径正确（/channel-api、/agent-api、/client-api），调用降级不崩溃。
5. TS 类型与 Entity 字段对齐。
6. uni-app 页面用 rpx + uni 组件，无 Element Plus 依赖。
