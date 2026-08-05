# 状态机管理 CRUD 功能设计

## 背景与目标

状态机引擎（`StateMachineEngine`）已在后端稳定运行，被权益/订单/服务会话/园区/内容/场景等 7+ 业务模块依赖，规则数据由 `system_state_machine` 表承载，启动时由 `StateMachineWarmUpRunner` 加载到 Redis 缓存。

当前缺失的是 **Admin 端的 CRUD 管理面**：运营无法在后台维护状态机规则，只能改 DB。前端页面 `system/stateMachine/index.vue` 已存在，但后端 Controller/Service 未实现，前端以 `silent: true` + try/catch 做降级。

**目标**：补齐管理面，让运营能在后台增删改查状态机规则，配置后立即对业务生效（刷新 Redis 缓存）。

## 范围

### 做
1. 后端新增 `SystemStateMachineService` + `SystemStateMachineAdminController`（照抄 `SystemConfigService`/`SystemConfigAdminController` 最简 CRUD 模式）。
2. CRUD 写操作后调用 `StateMachineEngine.refreshRules(machineCode)` 刷新缓存。
3. 前端类型对齐后端实体（`fromState/toState` 由 string 改 number，补齐可选字段，bizType 选项对齐 seed）。
4. 前端页面状态字段改数字输入，移除降级逻辑。

### 不做（YAGNI）
- 不把 `fromSubState/toSubState/conditionExpr/actionBean` 暴露到表单（DB 列可空，保持表单简洁）。
- 不改后端实体/DDL/引擎（已稳定，被业务依赖）。
- 不动 RBAC 权限分配（`system:sm:*` 权限码沿用，超管已自动有全部权限）。

## 用户决策记录

| 决策点 | 选择 |
|--------|------|
| 契约对齐方向 | 以后端为准则改前端 |
| 表单字段范围 | 只暴露 10 个核心字段，不暴露 subState/conditionExpr/actionBean |
| 缓存刷新策略 | CRUD 后自动调 `StateMachineEngine.refreshRules(machineCode)` |

## 数据契约

### 后端接口（`/admin-api/state-machines`）

| 方法 | 路径 | 权限码 | 入参 | 出参 |
|------|------|--------|------|------|
| GET | `/` | `system:sm:list` | `current,size,machineCode?,bizType?` | `R<PageResult<SystemStateMachine>>` |
| GET | `/{id}` | `system:sm:list` | `id` | `R<SystemStateMachine>` |
| POST | `/` | `system:sm:create` | `SystemStateMachine` body | `R<Long>` |
| PUT | `/{id}` | `system:sm:update` | `id` + body | `R<Void>` |
| DELETE | `/{id}` | `system:sm:delete` | `id` | `R<Void>` |

**路径用自增 id 而非业务键**：状态机唯一约束是复合键 `(machine_code, from_state, from_sub_state, event_code)`，不宜塞进 URL；用 id 简洁且符合 REST 习惯（与 SystemConfig 用 configKey 不同，但合理）。

### Service 层（`SystemStateMachineService`）

照抄 `SystemConfigService`：具体 `@Service` 类、无接口、无 impl 目录、`@RequiredArgsConstructor`、`LambdaQueryWrapper` + `Page`。

方法：
- `page(current, size, machineCode, bizType)` — machineCode/bizType 精确过滤，`orderByAsc(sortOrder).orderByAsc(fromState)`。
- `getById(id)` — `selectById`。
- `create(entity)` — 唯一性校验 `(machineCode, fromState, fromSubState, eventCode)` → insert → `engine.refreshRules(machineCode)`。
- `update(id, entity)` — 查旧记录 → `updateById` → 若 machineCode 变更则同时刷新旧域和新域，否则刷当前域。
- `delete(id)` — 先查 machineCode → delete → `engine.refreshRules(machineCode)`。

**唯一性校验**：对应 DDL 唯一键 `uk_machine_from_event(machine_code, from_state, from_sub_state, event_code)`。`create` 前用 `selectCount` 预校验，命中抛 `BusinessException(BUSINESS, "规则已存在...")`。`update` 靠 DB 约束兜底。

### 前端类型（`types/stateMachine.ts`）

```typescript
export const STATE_MACHINE_BIZ_TYPE_OPTIONS = [
  { label: '权益', value: 'equity' },
  { label: '订单', value: 'order' },
  { label: '服务会话', value: 'service' },
  { label: '园区', value: 'park' },
  { label: '内容', value: 'content' },
  { label: '场景', value: 'scene' }
] as const

export interface SystemStateMachine {
  id?: number
  machineCode: string
  machineName: string
  bizType: string
  fromState: number               // 由 string 改为 number
  fromStateName: string
  fromSubState?: string | null    // 新增（类型对齐，表单不暴露）
  toState: number                 // 由 string 改为 number
  toStateName: string
  toSubState?: string | null      // 新增
  eventCode: string
  eventName: string
  conditionExpr?: string | null   // 新增
  actionBean?: string | null      // 新增
  sortOrder: number
  status: number
  remark: string | null
  createdAt?: string
  updatedAt?: string
}
```

### 前端 API（`api/stateMachine.ts`）

- 移除全部 `silent: true`（后端已实现，不再需要降级）。
- 新增 `getStateMachine(id): Promise<SystemStateMachine>`。
- 更新顶部注释。

### 前端页面（`views/system/stateMachine/index.vue`）

- `fromState/toState` 表单项：`el-input` → `el-input-number`。
- `bizType` 选项跟随类型文件更新（去掉 refund，补 service/park/content）。
- 移除首屏降级逻辑（`available` flag + "接口待后端提供"提示）。
- 修复因类型变更（string→number）导致的表单初始化/提交代码。

## 实现步骤

### 后端

1. **新建 `SystemStateMachineService.java`**（`dayan-module-system/src/main/java/com/dayan/system/service/`）
   - 依赖注入 `SystemStateMachineMapper` + `StateMachineEngine`。
   - 实现 page/getById/create/update/delete。
   - 写方法加 `@Transactional(rollbackFor = Exception.class)`。
   - create/update/delete 末尾调 `engine.refreshRules(machineCode)`。

2. **新建 `SystemStateMachineAdminController.java`**（`.../controller/admin/`）
   - `@RequestMapping("/state-machines")`。
   - 5 个端点 + `@SaCheckPermission`。
   - 照抄 `SystemConfigAdminController` 结构。

3. **编译验证**：`mvn -pl dayan-modules/dayan-module-system -am compile`。

### 前端

4. **修改 `types/stateMachine.ts`**：类型对齐 + bizType 选项更新。

5. **修改 `api/stateMachine.ts`**：移除 silent、新增 getById、更新注释。

6. **修改 `views/system/stateMachine/index.vue`**：表单数字输入 + 移除降级 + 修复类型适配。

### 验证

7. **前端类型检查**：`pnpm vue-tsc`（0 error）。

8. **集成验证**（浏览器）：
   - 列表加载：显示 seed 6 台状态机规则。
   - 新增：写一条 ORDER_SM 规则 → DB 写入 + 缓存刷新。
   - 编辑：改 eventName → DB 更新 + 缓存刷新。
   - 删除：删刚新增的规则 → DB 删除 + 缓存刷新。
   - 唯一性校验：重复新增同 (machineCode, fromState, eventCode) → 报"规则已存在"。
   - 缓存实际生效：新增规则后触发业务流转 → 新规则生效。

## 风险与回滚

- **风险点**：`update` 时 machineCode 被改 → 需同时刷新旧域和新域。设计已覆盖。
- **回滚**：纯新增 + 类型对齐，无破坏性改动。后端新 Controller 不影响现有引擎；前端改动仅影响状态机管理页本身。删除新文件即回滚。

## 参考文件

- 后端 CRUD 范式：`SystemConfigAdminController.java` / `SystemConfigService.java`
- 实体/Mapper：`SystemStateMachine.java` / `SystemStateMachineMapper.java`
- 引擎接口：`StateMachineEngine.java`（refreshRules 方法）
- DDL：`db/migration/01_system.sql`（system_state_machine 表，line 100-128）
- Seed：`db/migration/seed/state_machine_seed.sql`（6 台机器 ~43 条规则）
- 菜单：`db/migration/seed/menu_seed.sql` line 32（admin_system_state_machine，权限 system:sm:list）
- 前端页面：`views/system/stateMachine/index.vue`
- 前端类型：`types/stateMachine.ts`
- 前端 API：`api/stateMachine.ts`
