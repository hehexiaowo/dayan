# P8 设计规格：Admin 前端（基础架构 + 系统管理页）

> 阶段：P8 第 1 增量（Admin 端基础架构 + RBAC/系统管理 9 页）  
> 目录：`dayan-web-admin/`（Vue3 + TypeScript + Element Plus + Pinia + Vue Router + Axios，脚手架已有）  
> 参考：现有 `src/utils/request.ts`、`src/stores/user.ts`、`src/views/login/index.vue`、`src/layouts/default/index.vue`

---

## 0. 全局约束（实现者必读，逐字遵守）

### 0.1 技术栈与现有脚手架（已就绪，勿重写）
- Vue 3.4 + TypeScript 5.4 + Element Plus 2.7 + Pinia 2.2 + Vue Router 4.4 + Axios 1.7 + Sass + Vite 5.4
- 已有：`src/utils/request.ts`（axios 实例 + Admin-Token 注入 + R<T> 拆包响应拦截器）、`src/stores/user.ts`（token+userInfo+login/logout/getInfo）、`src/api/auth.ts`、`src/router/index.ts`（静态路由：login/dashboard/404）、`src/layouts/default/index.vue`（侧边栏+顶栏框架，当前菜单仅 dashboard 一项）、`src/permission.ts`（路由守卫）、`src/types/global.d.ts`（ApiResult/code 常量）、`src/types/auth.ts`。
- **不要重写上述已有文件**，除非任务明确要求（如 P8-A 改造 router 为动态路由、改造 layout 菜单为动态渲染）。

### 0.2 后端 API 契约（实际路径，非 API 文档的 v1 版）
**关键**：后端实际路径为 `/admin-api/{resource}`，**无 `/v1/` 版本前缀**（API 文档 05 的 `/admin-api/v1/...` 是设计稿，未落地）。`context-path=/admin-api` 由 dayan-admin 启动模块配置，控制器 `@RequestMapping("/xxx")` 即 `/admin-api/xxx`。

前端 baseURL 留空走 Vite 代理（`/admin-api` → `localhost:8000`），API 函数里 url 写 `/admin-api/xxx`（与现有 auth.ts 一致）。

### 0.3 响应拆包
`request.ts` 已对 `R<T>` 拆包：成功（code=0）返回 `data`，失败自动 ElMessage 报错并 reject。**API 函数返回类型直接写业务 data 类型**（如 `Promise<PageResult<Menu>>`，不是 `Promise<ApiResult<Menu>>`）。

### 0.4 后端 Controller 返回实体（非 VO）
现有 controller 多直接返回 Entity（如 `R<PageResult<OrganAccount>>`、`R<SystemMenu>`）。TS 类型按 Entity 字段定义（字段见 §3），分页返回 `PageResult<T>`（结构见 §1.3）。

### 0.5 Sa-Token 鉴权
后端用 `@SaCheckPermission("xxx")` 校验。前端：登录返回的 `isAdmin` 为 true 时拥有全部权限；非 admin 暂按全权处理（菜单/按钮权限的精细化前端控制留后续增量）。Token 请求头 `Admin-Token`（已在 request.ts 处理）。

### 0.6 编码风格
- `<script setup lang="ts">` 组合式 API。
- 类型放 `src/types/{domain}.ts`；API 放 `src/api/{domain}.ts`；页面放 `src/views/{module}/`；可复用组件放 `src/components/`。
- 组件名、文件名用 PascalCase（组件）/ kebab-case（路由 path）。
- Element Plus 组件全量已注册（见 main.ts，确认；若未全量注册则在页面按需 import）。

### 0.7 验证
- `cd dayan-web-admin && npm install`（首次，node v24）。
- 类型检查：`npm run type-check`（vue-tsc --noEmit）必须 0 error。
- 构建：`npm run build`（vue-tsc + vite build）必须成功。
- 不要求真实后端联调（后端未启动），API 调用层写对契约即可。

---

## 1. P8-A：前端基础架构（控制者+1 子智能体）

### 1.1 动态菜单与路由（改造现有 router + layout）
现状：`router/index.ts` 静态路由（dashboard 一项），`layout` 菜单硬编码 dashboard。

目标：**从后端 `/admin-api/menus/tree?domainType=admin` 拉取菜单树，动态生成侧边栏 + 异步路由**。

- `src/api/menu.ts`：`getMenuTree(domainType?: string): Promise<Menu[]>` → GET `/admin-api/menus/tree`。
- `src/types/menu.ts`：`Menu` 接口（字段对齐 SystemMenu 实体：menuCode/menuName/parentCode/menuType/path/component/permissionCode/icon/sortOrder/isVisible/isExternal/isCache/domainType/status/children?）。
- `src/router/index.ts` 改造：保留 login/404 静态路由；`/` 下 dashboard 作为默认子路由；**新增动态路由生成函数** `generateRoutes(menus: Menu[]): RouteRecordRaw[]`，把 menuType=1(目录)/2(菜单) 的项转为路由，component 字段用 `() => import()` 动态导入（映射规则：component 值如 `views/system/dict/index` → `() => import(`@/views/system/dict/index.vue`)`）。menuType=3(按钮) 不生成路由。
- `src/permission.ts` 改造：登录后/刷新时，在守卫里 `await userStore.getInfo()` 后调 `getMenuTree('admin')`，`generateRoutes` 后 `router.addRoute` 动态挂载，再 `next({ ...to, replace: true })`。
- `src/layouts/default/index.vue` 改造：侧边栏 `el-menu` 从 store 里的菜单树递归渲染（`<el-sub-menu>` 目录 + `<el-menu-item>` 菜单），支持折叠、icon（menu.icon 用 Element Plus 图标组件名动态渲染）。保留现有顶栏/用户下拉/退出逻辑。

> **菜单数据来源**：本期菜单树由后端 system_menu 表提供（P1 已 seed）。若 seed 中 Admin 菜单不完整，layout 仍需能正常渲染存在的菜单项 + 至少 dashboard。**动态路由对找不到 component 文件的项做 try/catch 跳过**，不阻塞整体。

### 1.2 API 基类与分页工具
- `src/api/_base.ts` 或直接在各 api 文件复用 `request<T>`（已有）。无需额外封装。
- `src/types/common.ts`：`PageResult<T>`（current/size/total/records）、`PageQuery`（current/size）。

### 1.3 公共 CRUD 组合式函数（可选但推荐）
- `src/composables/useCrud.ts`：封装 page/create/update/delete 通用调用 + loading 状态 + ElMessage 成功提示，减少页面样板。签名参考：
  ```ts
  function useCrud<T, Q extends PageQuery>(api: {
    page: (q: Q) => Promise<PageResult<T>>
    create?: (data: T) => Promise<string>
    update?: (code: string, data: T) => Promise<void>
    remove?: (code: string) => Promise<void>
  }) { /* loading, tableData, total, handlePage/handleCreate/handleUpdate/handleDelete */ }
  ```

### 1.4 App 主入口
确认 `src/main.ts` 注册了 Element Plus（全量或按需）、Pinia、Router、图标。若图标未全局注册，补 `@element-plus/icons-vue` 全量注册（layout 动态 icon 需要）。

---

## 2. P8-B：RBAC 管理页（6 页，子智能体）

每个页面 = 1 个 view + 复用 api/types。路径前缀 `/admin-api`。

### 2.1 账号管理 `/system/account`
- API（`src/api/account.ts`）：page(organCode,username?,realName?,accountStatus?,current,size) / getDetail(accountCode) / create(account) / update(accountCode,account) / resetPassword(accountCode) / switchStatus(accountCode,status) / delete(accountCode)。
- 路由 `/system/account`，对应 GET `/accounts`。
- 页面：搜索栏（username/realName/accountStatus select + organCode，organCode 暂用默认 organ 或输入）+ el-table（accountCode/username/realName/phone/accountStatus 标签/操作）+ 新增/编辑弹窗 el-form + 分页。状态切换用 el-switch。重置密码二次确认。
- 类型（`src/types/account.ts`）：Account 接口（organCode/accountCode/username/realName/avatar/gender/phone/email/accountStatus/isAdmin/remark + lastLoginTime/loginCount，**不含 password/salt/idCard/openId 等敏感字段在表单**；create 时 password 必填，update 时 password 留空不改）。

### 2.2 角色管理 `/system/role`
- API（`src/api/role.ts`）：page / getDetail(roleCode) / create / update(roleCode,role) / delete(roleCode) / getPermissions(roleCode) / updatePermissions(roleCode,codes[])。
- 页面：el-table（roleCode/roleName/roleType/status/操作）+ 新增/编辑弹窗 + **权限分配弹窗**（el-tree 勾选权限，数据来自 GET `/permissions/tree`）。dataScope 字段用 select。
- 类型：Role（organCode/roleCode/roleName/roleType/description/dataScope/status/sortOrder/permissionCodes[]）。

### 2.3 权限管理 `/system/permission`
- API（`src/api/permission.ts`）：list / listAll / tree / create / update(permissionCode,perm) / delete(permissionCode)。
- 页面：**树形表格**（el-table tree-props，permissionType 1菜单/2按钮/3接口）+ 新增/编辑弹窗。
- 类型：Permission（permissionCode/permissionName/parentCode/permissionType/path/method/icon/sortOrder/status/remark/children?）。

### 2.4 菜单管理 `/system/menu`
- API（`src/api/menu.ts`，P8-A 已建 getMenuTree，此处补 CRUD）：list(domainType?) / tree(domainType?) / create / update(menuCode,menu) / delete(menuCode)。
- 页面：**树形表格**（menuType 1目录/2菜单/3按钮）+ domainType 筛选（admin/channel/agent/client）+ 新增/编辑弹窗（parentCode 用 tree-select）。
- 类型：Menu（见 §1.1）。

### 2.5 部门管理 `/system/department`
- API（`src/api/department.ts`）：list(organCode) / create / update(organCode,deptCode,dept) / delete(organCode,deptCode)。
- 页面：树形表格（parentCode 层级）+ 新增/编辑弹窗。
- 类型：Department（organCode/deptCode/deptName/parentCode/deptType/leaderName/leaderPhone/sortOrder/status/remark/children?）。

### 2.6 员工管理 `/system/employee`
- API（`src/api/employee.ts`）：page / listByDept(organCode,deptCode) / create / update(organCode,employeeCode,emp) / delete(organCode,employeeCode)。
- 页面：搜索 + el-table + 弹窗（含 deptCode 选择）。
- 类型：Employee（organCode/employeeCode/accountCode/deptCode/realName/gender/phone/email/position/entryDate/employeeStatus/remark）。

---

## 3. P8-C：系统配置页（3 页，子智能体）

### 3.1 字典查看 `/system/dict`
- API（`src/api/dict.ts`）：listByType(dictType) → GET `/dicts/type/{dictType}` / getDetail(dictType,dictCode) → GET `/dicts/{dictType}/{dictCode}`。
- **注意**：后端字典 controller 仅查询无 CRUD（字典由 seed 初始化）。页面以**只读展示**为主：左侧 dictType 列表（equity_status/order_status/pay_type/性别 等）+ 右侧选中类型的字典项 el-table。
- 类型：Dict（dictType/dictCode/dictName/dictValue/parentCode/level/sortOrder/status/isDefault/remark）。

### 3.2 系统配置 `/system/config`
- API（`src/api/config.ts`）：page(group?) → GET `/configs` / listByGroup(group) → GET `/configs/group/{group}` / create / update(configKey,config) / delete(configKey)。
- 页面：configGroup 筛选 + el-table（configKey/configName/configValue 值/env/scope/操作）+ 编辑弹窗（isSecret=1 的值脱敏显示）。
- 类型：Config（configGroup/configKey/configValue/valueType/env/scope/configName/description/isSecret/isRuntime/sortOrder）。

### 3.3 操作日志 `/system/log`（后端 controller 暂缺时的降级）
- **现状**：system 模块无 SystemOperationLogAdminController。本期页面**做 UI 骨架 + API 预留**：API 函数写好契约（page 查询 system_operation_log），但因后端 404，页面调用会报错——在页面 onMounted 用 try/catch 包裹，失败时显示空状态 + "日志接口待后端提供"提示。
- 类型：OperationLog（按 system_operation_log 实体字段：logType/module/action/method/requestUrl/requestParams/responseResult/status/costTime/operatorCode/operatorName/operatorIp/operateTime）。
- **裁决**：不阻塞 P8 验收，记录为"待后端 P8 补丁"。

---

## 4. 任务拆分

| 任务 | 内容 | 执行 |
|------|------|------|
| P8-spec | 本规格 | 完成 |
| P8-A | 基础架构：动态菜单/路由 + layout 改造 + main.ts 图标注册 + composables/useCrud + types/common | 控制者协调，可 1 子智能体 |
| P8-B | RBAC 6 页（账号/角色/权限/菜单/部门/员工）+ 对应 api/types | 1 子智能体（标准模型） |
| P8-C | 系统配置 3 页（字典/配置/日志）+ 对应 api/types | 1 子智能体（标准模型） |
| P8-verify | `npm install` + `npm run type-check` + `npm run build` | 控制者 |

**并行性**：P8-B（views/system/* + api/types）与 P8-C（views/system/dict,config,log）文件有重叠目录（都在 views/system/），但具体文件不重叠（B: account/role/permission/menu/department/employee；C: dict/config/log）。**可并行**，但需约定：B 只动其 6 页文件，C 只动其 3 页文件，共享的 types/common.ts 由 P8-A 先建好。**串行**：A → (B ‖ C) → verify。

---

## 5. 验收标准

1. `npm run type-check` 0 error（vue-tsc 通过）。
2. `npm run build` 成功产出 dist/。
3. 动态菜单：登录后 layout 侧边栏能渲染后端返回的菜单树（至少 dashboard）；路由守卫正确加载动态路由。
4. 9 个页面路由可达，CRUD 操作调用正确后端契约路径（/admin-api/xxx，无 v1）。
5. TS 类型与后端 Entity 字段对齐。
6. 敏感字段（password/salt/idCard/openId）不在列表/编辑表单暴露（create 时 password 除外）。
7. 操作日志页因后端缺接口做降级处理（try/catch + 空状态），不阻塞构建。
