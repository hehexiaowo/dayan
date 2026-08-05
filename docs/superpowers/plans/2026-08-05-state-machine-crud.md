# 状态机管理 CRUD 功能实现计划

> **面向 AI 代理的工作者：** 必需子技能：使用 superpowers:subagent-driven-development（推荐）或 superpowers:executing-plans 逐任务实现此计划。步骤使用复选框（`- [ ]`）语法来跟踪进度。

**目标：** 补齐状态机规则的后端 CRUD 管理面 + 前端契约对齐，让运营能在 Admin 后台维护状态机规则并立即生效。

**架构：** 后端新增 `SystemStateMachineService`（具体类，照抄 `SystemConfigService` 模式）+ `SystemStateMachineAdminController`（5 个 REST 端点），CRUD 写操作后调 `StateMachineEngine.refreshRules(machineCode)` 刷新 Redis 缓存。前端类型对齐后端实体（`fromState/toState` 由 string 改 number），表单改数字输入，移除降级逻辑。

**技术栈：** Spring Boot + MyBatis-Plus + Sa-Token（后端）；Vue 3 + Element Plus + TypeScript（前端）

**设计文档：** `docs/superpowers/specs/state-machine-crud.md`

**验证策略说明：** 本项目无单元测试基础设施（后端无 `spring-boot-starter-test`，前端无 `vitest`，既有 SystemConfig 模块也无测试类）。遵循项目既有规范，采用「编译通过 + 类型检查通过 + 浏览器端到端验证」三重保障，与既有模块一致。每个写操作任务都包含编译/类型检查验证步骤。

---

## 文件结构

### 创建
| 文件 | 职责 |
|------|------|
| `dayan-server/dayan-modules/dayan-module-system/src/main/java/com/dayan/system/service/SystemStateMachineService.java` | 状态机规则 CRUD 业务逻辑 + 缓存刷新 |
| `dayan-server/dayan-modules/dayan-module-system/src/main/java/com/dayan/system/controller/admin/SystemStateMachineAdminController.java` | Admin 端 REST 接口（5 端点） |

### 修改
| 文件 | 改动 |
|------|------|
| `dayan-web-admin/src/types/stateMachine.ts` | `fromState/toState` string→number；补 4 个可选字段；bizType 选项对齐 seed |
| `dayan-web-admin/src/api/stateMachine.ts` | 移除 `silent: true`；新增 `getStateMachine`；更新注释 |
| `dayan-web-admin/src/views/system/stateMachine/index.vue` | 状态字段改 `el-input-number`；移除降级逻辑；修复类型适配 |

---

## 任务 1：后端 Service 层

**文件：**
- 创建：`dayan-server/dayan-modules/dayan-module-system/src/main/java/com/dayan/system/service/SystemStateMachineService.java`

- [ ] **步骤 1：编写 `SystemStateMachineService.java` 完整实现**

创建文件 `dayan-server/dayan-modules/dayan-module-system/src/main/java/com/dayan/system/service/SystemStateMachineService.java`，内容如下：

```java
package com.dayan.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dayan.common.core.exception.BusinessException;
import com.dayan.common.core.exception.ErrorCode;
import com.dayan.common.core.resp.PageResult;
import com.dayan.common.core.statemachine.StateMachineEngine;
import com.dayan.system.entity.SystemStateMachine;
import com.dayan.system.mapper.SystemStateMachineMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 状态机规则配置服务。
 *
 * <p>负责 system_state_machine 表的 CRUD，写操作后刷新引擎缓存，
 * 保证运营在 Admin 端的配置变更立即对业务生效。
 */
@Service
@RequiredArgsConstructor
public class SystemStateMachineService {

    private final SystemStateMachineMapper stateMachineMapper;
    private final StateMachineEngine stateMachineEngine;

    /**
     * 分页查询（按 machineCode / bizType 过滤）。
     */
    public PageResult<SystemStateMachine> page(long current, long size, String machineCode, String bizType) {
        LambdaQueryWrapper<SystemStateMachine> wrapper = new LambdaQueryWrapper<SystemStateMachine>()
                .orderByAsc(SystemStateMachine::getSortOrder)
                .orderByAsc(SystemStateMachine::getFromState);
        if (machineCode != null && !machineCode.isEmpty()) {
            wrapper.eq(SystemStateMachine::getMachineCode, machineCode);
        }
        if (bizType != null && !bizType.isEmpty()) {
            wrapper.eq(SystemStateMachine::getBizType, bizType);
        }
        Page<SystemStateMachine> page = stateMachineMapper.selectPage(new Page<>(current, size), wrapper);
        return new PageResult<>(current, size, page.getTotal(), page.getRecords());
    }

    /**
     * 按 id 查单条。
     */
    public SystemStateMachine getById(Long id) {
        SystemStateMachine entity = stateMachineMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "状态机规则不存在: " + id);
        }
        return entity;
    }

    /**
     * 新增规则。
     *
     * <p>唯一性校验对应 DDL 唯一键 uk_machine_from_event(machine_code, from_state, from_sub_state, event_code)。
     * 新增后刷新该 machineCode 的缓存。
     */
    @Transactional(rollbackFor = Exception.class)
    public Long create(SystemStateMachine entity) {
        checkUnique(entity);
        stateMachineMapper.insert(entity);
        stateMachineEngine.refreshRules(entity.getMachineCode());
        return entity.getId();
    }

    /**
     * 修改规则。
     *
     * <p>若 machineCode 被修改，旧域和新域的缓存都要刷新（旧域规则可能减少，新域规则增加）。
     */
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, SystemStateMachine entity) {
        SystemStateMachine existing = stateMachineMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "状态机规则不存在: " + id);
        }
        String oldMachineCode = existing.getMachineCode();
        entity.setId(id);
        stateMachineMapper.updateById(entity);
        // 当前 machineCode 域一定刷新
        stateMachineEngine.refreshRules(entity.getMachineCode());
        // 若 machineCode 变更，旧域也要刷新（旧域规则减少）
        if (entity.getMachineCode() != null && !entity.getMachineCode().equals(oldMachineCode)) {
            stateMachineEngine.refreshRules(oldMachineCode);
        }
    }

    /**
     * 删除规则。
     *
     * <p>删除前先查出 machineCode，删除后刷新该域缓存。
     */
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        SystemStateMachine existing = stateMachineMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "状态机规则不存在: " + id);
        }
        stateMachineMapper.deleteById(id);
        stateMachineEngine.refreshRules(existing.getMachineCode());
    }

    /**
     * 唯一性校验：(machineCode, fromState, fromSubState, eventCode) 组合不可重复。
     */
    private void checkUnique(SystemStateMachine entity) {
        LambdaQueryWrapper<SystemStateMachine> wrapper = new LambdaQueryWrapper<SystemStateMachine>()
                .eq(SystemStateMachine::getMachineCode, entity.getMachineCode())
                .eq(SystemStateMachine::getFromState, entity.getFromState())
                .eq(SystemStateMachine::getEventCode, entity.getEventCode());
        // fromSubState 可能为 null，MyBatis-Plus 的 eq(null) 会生成 IS NULL
        if (entity.getFromSubState() == null || entity.getFromSubState().isEmpty()) {
            wrapper.and(w -> w.isNull(SystemStateMachine::getFromSubState)
                    .or().eq(SystemStateMachine::getFromSubState, ""));
        } else {
            wrapper.eq(SystemStateMachine::getFromSubState, entity.getFromSubState());
        }
        Long count = stateMachineMapper.selectCount(wrapper);
        if (count > 0) {
            throw new BusinessException(ErrorCode.BUSINESS,
                    "规则已存在: machineCode=" + entity.getMachineCode()
                            + ", fromState=" + entity.getFromState()
                            + ", eventCode=" + entity.getEventCode());
        }
    }
}
```

**设计要点说明：**
- `checkUnique` 对 `fromSubState` 的 null 值做了特殊处理：DDL 唯一键在 `from_sub_state` 为 NULL 时不触发唯一约束（MySQL NULL 语义），但 Service 层预校验仍需正确匹配空值，用 `isNull OR eq('')` 覆盖 null 和空串两种情况。
- `update` 不做唯一性预校验（靠 DB 唯一键兜底），因为 update 走 `updateById` 改的是非主键字段，若触发唯一键冲突 MyBatis-Plus 会抛异常被全局异常处理器转成 BUSINESS 错误。
- `update` 判断 machineCode 变更时刷新双域，避免旧域缓存残留已迁出的规则。

- [ ] **步骤 2：编译验证 Service**

运行：
```bash
cd F:/code/dayan/dayan-server && mvn -pl dayan-modules/dayan-module-system -am compile -q
```
预期：BUILD SUCCESS，无编译错误。

若报错，常见原因：
- `StateMachineEngine` 包路径错误 → 确认是 `com.dayan.common.core.statemachine.StateMachineEngine`
- `BusinessException` 构造签名 → 确认是 `new BusinessException(ErrorCode, String message)`

- [ ] **步骤 3：Commit**

```bash
cd F:/code/dayan && git add dayan-server/dayan-modules/dayan-module-system/src/main/java/com/dayan/system/service/SystemStateMachineService.java
git commit -m "feat(sm): 新增 SystemStateMachineService（CRUD + 缓存刷新）"
```

---

## 任务 2：后端 Controller 层

**文件：**
- 创建：`dayan-server/dayan-modules/dayan-module-system/src/main/java/com/dayan/system/controller/admin/SystemStateMachineAdminController.java`

**依赖：** 任务 1 完成（Service 已存在）

- [ ] **步骤 1：编写 `SystemStateMachineAdminController.java` 完整实现**

创建文件 `dayan-server/dayan-modules/dayan-module-system/src/main/java/com/dayan/system/controller/admin/SystemStateMachineAdminController.java`，内容如下：

```java
package com.dayan.system.controller.admin;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.dayan.common.core.resp.PageResult;
import com.dayan.common.core.resp.R;
import com.dayan.system.entity.SystemStateMachine;
import com.dayan.system.service.SystemStateMachineService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * Admin 端状态机规则配置接口。
 */
@Tag(name = "状态机配置")
@RestController
@RequestMapping("/state-machines")
@RequiredArgsConstructor
public class SystemStateMachineAdminController {

    private final SystemStateMachineService stateMachineService;

    @Operation(summary = "状态机规则分页")
    @SaCheckPermission("system:sm:list")
    @GetMapping
    public R<PageResult<SystemStateMachine>> page(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "20") long size,
            @RequestParam(required = false) String machineCode,
            @RequestParam(required = false) String bizType) {
        return R.ok(stateMachineService.page(current, size, machineCode, bizType));
    }

    @Operation(summary = "状态机规则详情")
    @SaCheckPermission("system:sm:list")
    @GetMapping("/{id}")
    public R<SystemStateMachine> getById(@PathVariable Long id) {
        return R.ok(stateMachineService.getById(id));
    }

    @Operation(summary = "新增状态机规则")
    @SaCheckPermission("system:sm:create")
    @PostMapping
    public R<Long> create(@RequestBody SystemStateMachine entity) {
        return R.ok(stateMachineService.create(entity));
    }

    @Operation(summary = "修改状态机规则")
    @SaCheckPermission("system:sm:update")
    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @RequestBody SystemStateMachine entity) {
        stateMachineService.update(id, entity);
        return R.ok();
    }

    @Operation(summary = "删除状态机规则")
    @SaCheckPermission("system:sm:delete")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        stateMachineService.delete(id);
        return R.ok();
    }
}
```

**设计要点说明：**
- `@RequestMapping("/state-machines")`：前缀 `/admin-api` 由全局配置拼接（与 SystemConfig 的 `/configs` 一致，Controller 只写后半段）。
- 路径变量用自增 `id` 而非业务键（与 SystemConfig 用 configKey 不同），因为状态机唯一约束是复合键，不宜塞进 URL。详见设计文档 §数据契约。
- 权限码 `system:sm:list/create/update/delete`，与 seed 菜单 `admin_system_state_machine` 的 `system:sm:list` 一致，超管已自动有全部权限。

- [ ] **步骤 2：编译验证全模块**

运行：
```bash
cd F:/code/dayan/dayan-server && mvn -pl dayan-modules/dayan-module-system -am compile -q
```
预期：BUILD SUCCESS。

- [ ] **步骤 3：Commit**

```bash
cd F:/code/dayan && git add dayan-server/dayan-modules/dayan-module-system/src/main/java/com/dayan/system/controller/admin/SystemStateMachineAdminController.java
git commit -m "feat(sm): 新增 SystemStateMachineAdminController（5 端点 REST）"
```

---

## 任务 3：前端类型对齐

**文件：**
- 修改：`dayan-web-admin/src/types/stateMachine.ts`

- [ ] **步骤 1：替换 `types/stateMachine.ts` 全文**

将 `dayan-web-admin/src/types/stateMachine.ts` 全文替换为：

```typescript
/**
 * 状态机配置相关类型。
 *
 * 字段对齐后端 com.dayan.system.entity.SystemStateMachine。
 */

/** 业务类型选项（对齐 seed 实际 6 个业务域） */
export const STATE_MACHINE_BIZ_TYPE_OPTIONS = [
  { label: '权益', value: 'equity' },
  { label: '订单', value: 'order' },
  { label: '服务会话', value: 'service' },
  { label: '园区', value: 'park' },
  { label: '内容', value: 'content' },
  { label: '场景', value: 'scene' }
] as const

/**
 * 状态机配置（后端 SystemStateMachine 实体）。
 *
 * 一条记录 = 状态机的一个「迁移规则」（fromState --event--> toState）。
 * fromState/toState 为 Integer（DDL TINYINT），对应业务状态枚举值。
 * fromSubState/toSubState/conditionExpr/actionBean 为高级字段，
 * 接口保留类型对齐，但 Admin 表单不暴露（DB 列可空）。
 */
export interface SystemStateMachine {
  id?: number
  /** 状态机编码（标识一组状态机，如 ORDER_SM） */
  machineCode: string
  /** 状态机名称 */
  machineName: string
  /** 业务类型：equity/order/service/park/content/scene */
  bizType: string
  /** 起始状态码（Integer，业务状态枚举值） */
  fromState: number
  /** 起始状态名称 */
  fromStateName: string
  /** 起始子状态值（高级字段，表单不暴露） */
  fromSubState?: string | null
  /** 目标状态码（Integer，业务状态枚举值） */
  toState: number
  /** 目标状态名称 */
  toStateName: string
  /** 目标子状态值（高级字段，表单不暴露） */
  toSubState?: string | null
  /** 触发事件编码（如 pay/cancel） */
  eventCode: string
  /** 触发事件名称 */
  eventName: string
  /** 流转条件表达式（高级字段，表单不暴露） */
  conditionExpr?: string | null
  /** 流转执行器 bean 名（高级字段，表单不暴露） */
  actionBean?: string | null
  /** 排序号 */
  sortOrder: number
  /** 状态：1启用 0禁用 */
  status: number
  /** 备注 */
  remark: string | null
  /** 创建时间 */
  createdAt?: string
  /** 更新时间 */
  updatedAt?: string
}

/** 状态机分页查询参数 */
export interface StateMachineQuery {
  /** 状态机编码筛选 */
  machineCode?: string
  /** 业务类型筛选 */
  bizType?: string
  current: number
  size: number
}
```

**变更点：**
1. `fromState/toState: string` → `number`（后端 Integer，DDL TINYINT）。
2. 新增 4 个可选字段：`fromSubState`、`toSubState`、`conditionExpr`、`actionBean`（类型对齐，表单不暴露）。
3. `STATE_MACHINE_BIZ_TYPE_OPTIONS`：去掉不存在的 `refund`，新增 `service/park/content`，顺序按 seed 出现频率（equity/order 在前）。
4. 移除文件顶部「后端 Controller 暂未提供」的注释（已实现）。

- [ ] **步骤 2：类型检查**

运行：
```bash
cd F:/code/dayan/dayan-web-admin && pnpm vue-tsc --noEmit
```
预期：此阶段页面文件（任务 5）还未改，可能报 fromState/toState 类型不匹配错误（string 赋给 number）。**这是预期的**——任务 5 修复页面后会消除。此步骤仅确认类型文件本身无语法错误。

若仅报 `stateMachine/index.vue` 中 `fromState/toState` 相关错误 → 正常，继续。
若报其他文件的类型错误 → 检查是否有其他文件引用了此类型。

- [ ] **步骤 3：Commit**

```bash
cd F:/code/dayan && git add dayan-web-admin/src/types/stateMachine.ts
git commit -m "feat(sm): 前端类型对齐后端实体（fromState/toState 改 number，补高级字段）"
```

---

## 任务 4：前端 API 层

**文件：**
- 修改：`dayan-web-admin/src/api/stateMachine.ts`

**依赖：** 任务 3 完成（类型已对齐）

- [ ] **步骤 1：替换 `api/stateMachine.ts` 全文**

将 `dayan-web-admin/src/api/stateMachine.ts` 全文替换为：

```typescript
import { request } from '@/utils/request'
import type { PageResult } from '@/types/common'
import type { SystemStateMachine, StateMachineQuery } from '@/types/stateMachine'

/**
 * 状态机配置接口封装。
 *
 * 对应后端 SystemStateMachineAdminController（/admin-api/state-machines/*）。
 */

/** 分页查询状态机配置：GET /admin-api/state-machines?machineCode&bizType&current&size */
export function pageStateMachines(query: StateMachineQuery): Promise<PageResult<SystemStateMachine>> {
  return request<PageResult<SystemStateMachine>>({
    url: '/admin-api/state-machines',
    method: 'get',
    params: {
      machineCode: query.machineCode || undefined,
      bizType: query.bizType || undefined,
      current: query.current,
      size: query.size
    }
  })
}

/** 查询状态机规则详情：GET /admin-api/state-machines/{id} */
export function getStateMachine(id: number): Promise<SystemStateMachine> {
  return request<SystemStateMachine>({
    url: `/admin-api/state-machines/${id}`,
    method: 'get'
  })
}

/** 新增状态机配置：POST /admin-api/state-machines */
export function createStateMachine(data: Partial<SystemStateMachine>): Promise<number> {
  return request<number>({
    url: '/admin-api/state-machines',
    method: 'post',
    data
  })
}

/** 修改状态机配置：PUT /admin-api/state-machines/{id} */
export function updateStateMachine(id: number, data: Partial<SystemStateMachine>): Promise<void> {
  return request<void>({
    url: `/admin-api/state-machines/${id}`,
    method: 'put',
    data
  })
}

/** 删除状态机配置：DELETE /admin-api/state-machines/{id} */
export function deleteStateMachine(id: number): Promise<void> {
  return request<void>({
    url: `/admin-api/state-machines/${id}`,
    method: 'delete'
  })
}
```

**变更点：**
1. 移除全部 `silent: true`（后端已实现，不再需要降级）。
2. 新增 `getStateMachine(id)` 对应 GET `/{id}` 端点。
3. `createStateMachine` 返回类型由 `Promise<string>` 改为 `Promise<number>`（后端返回 Long id）。
4. 更新顶部注释，移除「后端暂未提供」说明。

- [ ] **步骤 2：类型检查**

运行：
```bash
cd F:/code/dayan/dayan-web-admin && pnpm vue-tsc --noEmit
```
预期：API 层无新错误（页面错误仍存在，待任务 5 修复）。

- [ ] **步骤 3：Commit**

```bash
cd F:/code/dayan && git add dayan-web-admin/src/api/stateMachine.ts
git commit -m "feat(sm): API 移除降级 silent，新增 getStateMachine"
```

---

## 任务 5：前端页面适配

**文件：**
- 修改：`dayan-web-admin/src/views/system/stateMachine/index.vue`

**依赖：** 任务 3、4 完成（类型和 API 已对齐）

- [ ] **步骤 1：修改 `<script setup>` 部分**

将 `dayan-web-admin/src/views/system/stateMachine/index.vue` 的 `<script setup lang="ts">` 块（第 1-183 行）替换为以下内容：

```vue
<script setup lang="ts">
import { reactive, ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import {
  pageStateMachines,
  createStateMachine,
  updateStateMachine,
  deleteStateMachine
} from '@/api/stateMachine'
import {
  type SystemStateMachine,
  type StateMachineQuery,
  STATE_MACHINE_BIZ_TYPE_OPTIONS
} from '@/types/stateMachine'

const loading = ref(false)
const tableData = ref<SystemStateMachine[]>([])
const total = ref(0)

const query = reactive<StateMachineQuery>({
  machineCode: '',
  bizType: '',
  current: 1,
  size: 20
})

/** 拉取分页数据 */
async function loadData() {
  loading.value = true
  try {
    const res = await pageStateMachines({ ...query })
    tableData.value = res.records
    total.value = res.total
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  query.current = 1
  loadData()
}

function handlePageChange(page: number) {
  query.current = page
  loadData()
}

function handleSizeChange(size: number) {
  query.size = size
  query.current = 1
  loadData()
}

function handleReset() {
  query.machineCode = ''
  query.bizType = ''
  query.current = 1
  loadData()
}

// ---------------- 新增/编辑弹窗 ----------------
const dialogVisible = ref(false)
const dialogMode = ref<'create' | 'edit'>('create')
const editingId = ref<number | undefined>(undefined)
const formRef = ref<FormInstance>()
const submitting = ref(false)

function defaultForm(): SystemStateMachine {
  return {
    machineCode: '',
    machineName: '',
    bizType: 'order',
    fromState: 0,
    fromStateName: '',
    toState: 0,
    toStateName: '',
    eventCode: '',
    eventName: '',
    sortOrder: 0,
    status: 1,
    remark: null
  }
}

const form = reactive<SystemStateMachine>(defaultForm())

const rules: FormRules<SystemStateMachine> = {
  machineCode: [{ required: true, message: '请输入状态机编码', trigger: 'blur' }],
  machineName: [{ required: true, message: '请输入状态机名称', trigger: 'blur' }],
  bizType: [{ required: true, message: '请选择业务类型', trigger: 'change' }],
  fromState: [{ required: true, message: '请输入起始状态码', trigger: 'blur' }],
  toState: [{ required: true, message: '请输入目标状态码', trigger: 'blur' }],
  eventCode: [{ required: true, message: '请输入触发事件编码', trigger: 'blur' }]
}

function openCreate() {
  dialogMode.value = 'create'
  editingId.value = undefined
  Object.assign(form, defaultForm())
  dialogVisible.value = true
}

function openEdit(row: SystemStateMachine) {
  dialogMode.value = 'edit'
  editingId.value = row.id
  Object.assign(form, defaultForm(), row)
  dialogVisible.value = true
}

async function handleSubmit() {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
  } catch {
    return
  }

  submitting.value = true
  try {
    if (dialogMode.value === 'create') {
      await createStateMachine({ ...form })
      ElMessage.success('新增成功')
    } else if (editingId.value !== undefined) {
      await updateStateMachine(editingId.value, { ...form })
      ElMessage.success('修改成功')
    }
    dialogVisible.value = false
    loadData()
  } finally {
    submitting.value = false
  }
}

async function onDelete(row: SystemStateMachine) {
  if (row.id === undefined) {
    ElMessage.warning('记录缺少主键 id，无法删除')
    return
  }
  try {
    await ElMessageBox.confirm(`确定删除状态机规则「${row.fromStateName || row.fromState} → ${row.toStateName || row.toState}」？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch {
    return // 用户取消
  }
  await deleteStateMachine(row.id)
  ElMessage.success('删除成功')
  loadData()
}

onMounted(() => {
  loadData()
})
</script>
```

**变更点：**
1. 移除 `available` ref 及所有降级逻辑（`try/catch` 里的 `ElMessage.warning`、空状态提示）。
2. `defaultForm()` 中 `fromState/toState` 初值由 `''`（空串）改为 `0`（number 类型）。
3. `onDelete` 确认弹窗用 `fromStateName || fromState`（number）显示，更准确。
4. 移除 `handleSubmit` 和 `onDelete` 中的 catch 块——后端已实现，错误由全局响应拦截器统一提示（`request.ts` 的 `ElMessage.error`），无需页面再吞错。
5. 移除文件顶部「降级」注释块。

- [ ] **步骤 2：修改 template 部分（移除降级提示条 + 改 empty-text）**

在 template 中找到降级提示条 `<el-alert v-if="!available" ...>` 整块（原第 189-197 行），整块删除：

删除这部分：
```html
      <!-- 降级提示条 -->
      <el-alert
        v-if="!available"
        title="状态机配置接口待后端提供"
        type="warning"
        description="当前后端 SystemStateMachineAdminController 暂未实现，页面展示为空状态。接口就绪后将自动加载。"
        show-icon
        :closable="false"
        class="degrade-alert"
      />
```

然后将 el-table 的 empty-text 从：
```html
        :empty-text="available ? '暂无状态机配置' : '接口待后端提供'"
```
改为：
```html
        empty-text="暂无状态机配置"
```

- [ ] **步骤 3：修改 template 中状态码输入框为数字输入**

在 template 中找到起始状态码表单项（原第 341-343 行）：

```html
            <el-form-item label="起始状态码" prop="fromState">
              <el-input v-model="form.fromState" placeholder="如 pending" />
            </el-form-item>
```

替换为：
```html
            <el-form-item label="起始状态码" prop="fromState">
              <el-input-number
                v-model="form.fromState"
                :min="0"
                :step="1"
                :precision="0"
                controls-position="right"
                placeholder="状态枚举值"
                style="width: 100%"
              />
            </el-form-item>
```

同理找到目标状态码表单项（原第 354-356 行）：

```html
            <el-form-item label="目标状态码" prop="toState">
              <el-input v-model="form.toState" placeholder="如 paid" />
            </el-form-item>
```

替换为：
```html
            <el-form-item label="目标状态码" prop="toState">
              <el-input-number
                v-model="form.toState"
                :min="0"
                :step="1"
                :precision="0"
                controls-position="right"
                placeholder="状态枚举值"
                style="width: 100%"
              />
            </el-form-item>
```

**说明：** `:precision="0"` 确保只接受整数；`:min="0"` 因为业务状态枚举值都是非负整数（如权益 0-7、订单 0-7）。

- [ ] **步骤 4：移除 style 中无用的 degrade-alert 样式**

在 `<style scoped lang="scss">` 中找到 `.degrade-alert` 规则（原第 397-399 行）：

```scss
  .degrade-alert {
    margin-bottom: 16px;
  }
```

整块删除（已不再使用）。

- [ ] **步骤 5：类型检查**

运行：
```bash
cd F:/code/dayan/dayan-web-admin && pnpm vue-tsc --noEmit
```
预期：0 errors。此时所有类型不匹配问题都应消除（`fromState/toState` 已是 number，表单用 `el-input-number` 绑定 number）。

若仍有错误：
- `Type 'string' is not assignable to type 'number'` → 检查是否还有遗漏的 `''` 赋值给 fromState/toState。
- `el-input-number` 相关报错 → 确认 Element Plus 已全局注册（其他页面如 sortOrder 已用 el-input-number，应该没问题）。

- [ ] **步骤 6：Commit**

```bash
cd F:/code/dayan && git add dayan-web-admin/src/views/system/stateMachine/index.vue
git commit -m "feat(sm): 页面状态码改数字输入，移除降级逻辑"
```

---

## 任务 6：端到端集成验证

**依赖：** 任务 1-5 全部完成，前后端代码均已就位。

- [ ] **步骤 1：启动后端**

确认后端已重启加载新 Controller。若之前在运行，重启 dayan-server。

验证启动日志包含 StateMachineWarmUpRunner 加载规则的日志（说明引擎正常）。

- [ ] **步骤 2：启动前端**

确认前端 dev server 运行在 `http://localhost:5173`。若页面未热更新，刷新浏览器。

- [ ] **步骤 3：浏览器进入状态机页面**

以超管账号登录 Admin，进入「系统管理 → 状态机配置」（路由 `/system/state-machine`）。

**验证项 1 - 列表加载：**
- 预期：表格显示 seed 数据（6 台状态机的规则，如 ORDER_SM、EQUITY_SM 等）。
- 不应再出现降级提示条「接口待后端提供」。
- 若仍为空且无报错 → 检查后端 Controller 是否注册（查启动日志的 RequestMappingHandlerMapping）。

- [ ] **步骤 4：验证新增**

点击「新增规则」，填写：
- 状态机编码：`ORDER_SM`
- 状态机名称：`订单状态机`
- 业务类型：`订单`
- 起始状态码：`1`（数字输入）
- 起始状态名：`测试源`
- 目标状态码：`2`
- 目标状态名：`测试目标`
- 事件编码：`test_event`
- 事件名称：`测试事件`

点击确定。
- 预期：提示「新增成功」，弹窗关闭，列表刷新出现新规则。

- [ ] **步骤 5：验证唯一性校验**

再次新增完全相同的一规则（machineCode=ORDER_SM, fromState=1, eventCode=test_event）。
- 预期：提示「规则已存在: machineCode=ORDER_SM, fromState=1, eventCode=test_event」（全局拦截器弹 error toast）。
- 弹窗不关闭。

- [ ] **步骤 6：验证编辑**

对刚新增的规则点「编辑」，把事件名称改为「测试事件-改」，点确定。
- 预期：提示「修改成功」，列表刷新，eventName 更新。

- [ ] **步骤 7：验证删除**

对刚新增的规则点「删除」，确认弹窗点「确定」。
- 预期：提示「删除成功」，列表刷新，该规则消失。

- [ ] **步骤 8：验证缓存刷新（关键）**

这是验证 CRUD 后缓存确实刷新的核心步骤。

方案 A（推荐，业务侧验证）：
1. 新增一条 ORDER_SM 规则：fromState=0, eventCode=e2e_test_refresh, toState=99, eventName=E2E测试。
2. 通过业务接口或 DB 检查 Redis 缓存 `dayan:sm:rule:ORDER_SM` 的 `0:e2e_test_refresh` 字段值应为 `99`。
   - 命令：`redis-cli hget dayan:sm:rule:ORDER_SM "0:e2e_test_refresh"`
3. 删除该规则后，该字段应从 Redis 消失。

方案 B（日志验证）：
- 后端日志应出现 `refreshRules(ORDER_SM)` 调用记录（若引擎有日志）。

- [ ] **步骤 9：最终验证提交**

所有验证通过后，确认工作区干净：
```bash
cd F:/code/dayan && git status
```
预期：clean（所有改动已在前 5 个任务中 commit）。

---

## 自检结果

**1. 规格覆盖度检查：**

| spec 章节 | 对应任务 |
|-----------|----------|
| 后端 Service（page/getById/create/update/delete + refreshRules） | 任务 1 |
| 后端 Controller（5 端点 + SaCheckPermission） | 任务 2 |
| 唯一性校验（复合键 + fromSubState null 处理） | 任务 1 步骤 1 的 checkUnique |
| 缓存刷新（create/update/delete 后 + update 双域） | 任务 1 步骤 1 的各方法 |
| 前端类型对齐（fromState/toState number + 4 可选字段 + bizType 选项） | 任务 3 |
| 前端 API（移除 silent + 新增 getStateMachine） | 任务 4 |
| 前端页面（数字输入 + 移除降级） | 任务 5 |
| 集成验证（列表/新增/编辑/删除/唯一性/缓存） | 任务 6 |

✅ 无遗漏。

**2. 占位符扫描：** 每个步骤都包含完整代码块或精确命令，无 TODO/待定/「类似上文」。✅

**3. 类型一致性检查：**
- Service 方法签名 `page(long,long,String,String)` / `getById(Long)` / `create(SystemStateMachine):Long` / `update(Long, SystemStateMachine)` / `delete(Long)` → Controller 调用一致 ✅
- 前端 `SystemStateMachine.fromState: number` → 页面 `el-input-number` 绑定 + `defaultForm()` 返回 `0` ✅
- 前端 `createStateMachine: Promise<number>` 与后端 `R<Long>` 对齐 ✅
- 前端 `getStateMachine(id: number)` 对应 Controller `GET /{id}` ✅

✅ 全部一致。
