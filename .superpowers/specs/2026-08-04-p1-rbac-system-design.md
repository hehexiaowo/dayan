# P1 系统域 + 核心域 RBAC - 设计规格

> **版本**：v1.0
> **编制日期**：2026-08-04
> **适用范围**：大雁养老 P1 阶段（系统域 + 核心域，后端核心优先）
> **依据**：`docs/08项目计划书.md` §2.3 P1、`.superpowers/specs/2026-08-04-p0-scaffold-design.md` §14 P1 衔接
> **前置**：P0 已完成（common 全模块、127 表 DDL、四端登录、状态机引擎、6 启动模块、种子数据）

---

## 一、范围决策（后端核心优先）

P1 计划书含 13 类任务，按"后端核心优先"策略聚焦 RBAC 鉴权链路 + 基础管理能力，短信/消息模板/配置热更新等非核心项后置：

### P1 本阶段（必做）

| 序号 | 模块 | 内容 | 优先级 |
|------|------|------|--------|
| 1 | **RBAC 权限体系** | 角色/权限/账号-角色关联 CRUD + Sa-Token 权限注解启用 + StpInterface 实现 | P0 |
| 2 | **状态机接入** | StateRuleLoader 实现（读 system_state_machine）+ 应用启动预热 4 状态机规则 | P0 |
| 3 | **组织架构** | organ_info/department/employee CRUD（树形） | P0 |
| 4 | **账号管理** | organ_account CRUD + 登录失败 5 次锁定 30 分钟（Redis 计数） | P0 |
| 5 | **操作日志落库** | OperationLogPublisher 实现（异步写 system_operation_log） | P1 |
| 6 | **字典缓存** | system_dict 缓存（Redis）+ 变更刷新 | P1 |
| 7 | **菜单管理** | system_menu CRUD（树形 + 四端可见性） | P1 |
| 8 | **系统配置** | system_config 基础 CRUD（global 级，多级热更新后置） | P1 |

### P1 后置（本阶段不做，留 P1.5 或 P2 补）

- 短信模板管理（多服务商抽象）→ P2 渠道域阶段补
- 系统消息（5 种类型 × 6 种 target）→ P2 阶段补
- 多级配置热更新（Nacos pub-sub）→ P3 阶段补
- 地域字典/IP 地域字典 → P2 阶段补
- Admin 前端系统管理页面 → 后端 API 就绪后增量补（建议 P1.5 专项）

---

## 二、RBAC 权限体系设计（核心）

### 2.1 RBAC 模型

基于 organ 域 5 张表（P0 已建表 + 种子超管角色）：

```
organ_account ──< organ_account_role_rel >── organ_role ──< organ_role_permission_ship >── organ_permission
                                                     │
                                                     └──< organ_role_menu_rel >── system_menu
```

- `organ_account`：账号（P0 已有超管 admin）
- `organ_role`：角色（P0 已有 ROLE_SUPER_ADMIN）
- `organ_permission`：权限项（4 类：菜单/按钮/接口/数据，permission_code 全局唯一）
- `organ_account_role_rel`：账号-角色多对多
- `organ_role_permission_ship`：角色-权限多对多
- `organ_role_menu_rel`：角色-菜单多对多

### 2.2 StpInterface 实现（Sa-Token 权限查询）

在 dayan-module-organ 实现 `StpInterface`，Sa-Token 的 `@SaCheckPermission` / `@SaCheckRole` 注解通过它查询当前登录人的权限码/角色码：

```java
@Component
public class DayanStpInterface implements StpInterface {
    public List<String> getPermissionList(Object loginId, String loginType) {
        // 按 accountCode → account_role_rel → role → role_permission_ship → permission
        // 超管（is_admin=1）返回 ["*"] 通配
    }
    public List<String> getRoleList(Object loginId, String loginType) {
        // 按 accountCode → account_role_rel → role
        // 超管返回 ["ROLE_SUPER_ADMIN"]
    }
}
```

**注意**：Sa-Token 多端模式下，每个 StpLogic（ADMIN/CHANNEL/AGENT/CLIENT）各自查询 StpInterface。本阶段先实现 Admin 端（organ 域），其余端 RBAC 在对应域阶段补。

### 2.3 权限码规范

`{domain}:{module}:{action}`，如：
- `organ:account:list` / `organ:account:create` / `organ:account:delete`
- `organ:role:assign` 角色分配
- `system:dict:list` / `system:menu:create`
- `park:info:list`（P3 机构域，P1 不涉及）

超管 `*` 通配，跳过细粒度校验。

### 2.4 数据权限（P1 占位）

organ_role.data_scope 四级（全部/本部门及下级/本部门/仅本人），P1 仅定义枚举 + 注解占位，真实 SQL 拦截在 P2+ 补（需结合部门 ancestors 祖级链）。

---

## 三、状态机接入设计

### 3.1 StateRuleLoader 实现

P0 已定义 `StateRuleLoader` 接口（common-core）+ `DefaultStateMachineEngine`（common-redis），P1 在 dayan-module-system 实现数据源：

```java
@Component
@RequiredArgsConstructor
public class SystemStateRuleLoader implements StateRuleLoader {
    private final SystemStateMachineMapper mapper;

    public List<StateRule> loadByDomain(String domain) {
        // SELECT from_state, event_code, to_state FROM system_state_machine WHERE machine_code=? AND status=1
        // 转 StateRule（fromStatus=from_state, event=event_code, toStatus=to_state）
    }
    public List<StateRule> loadAll() { /* 全量 */ }
}
```

### 3.2 启动预热

应用启动后（ApplicationRunner / @PostConstruct）调用 `engine.loadAllRules()`，把 36 条规则加载到 Redis Hash。

---

## 四、组织架构设计

### 4.1 organ_info（公司/分公司）

- 树形结构（parent_code）
- CRUD + 营业执照上传（OSS 后置，P1 仅存 URL）
- 信用代码唯一校验

### 4.2 organ_department（部门）

- 多级（parent_code + ancestors 祖级链，如 "OR00001,DEPT001,DEPT002"）
- 新增/移动部门时维护 ancestors 完整性
- 部门人数统计（count employee）

### 4.3 organ_employee（员工）

- 员工档案 CRUD，与 organ_account 1:1（account_code 关联）
- 工号唯一校验

---

## 五、账号管理设计

### 5.1 organ_account CRUD

- 新增账号：生成 account_code（CodeGenerator）+ BCrypt 哈希密码 + 关联角色
- 编辑：不可改密码（单独接口）
- 重置密码：管理员重置为默认密码（BCrypt）
- 状态切换：正常/锁定/禁用

### 5.2 登录失败锁定（增强 P0 登录）

P0 登录无失败锁定，P1 补：
- 登录失败 → Redis INCR `dayan:auth:fail:admin:{username}`，TTL 30 分钟
- 达 5 次 → 抛 AccountLockedException，账号锁定 30 分钟
- 登录成功 → 清除失败计数

---

## 六、操作日志落库

### 6.1 OperationLogPublisher 实现

dayan-module-system 实现 `OperationLogPublisher`（common-log 接口）：

```java
@Component
@RequiredArgsConstructor
public class SystemOperationLogPublisher implements OperationLogPublisher {
    private final SystemOperationLogMapper mapper;

    @Async
    public void publish(OperationLogRecord record) {
        SystemOperationLog entity = convert(record);
        mapper.insert(entity);
    }
}
```

P0 的 `@OperationLog` 注解 + AOP 切面已就绪，P1 补落库实现即生效。

---

## 七、字典缓存设计

### 7.1 DictService

- `getByType(dictType)` → 先查 Redis（`dayan:dict:{type}`），未命中查 DB 并缓存
- 增删改后删除对应 type 的缓存键
- 启动预热常用 type（gender/status/account_status 等）

### 7.2 缓存结构

Redis Hash `dayan:dict:{type}`，field=dict_code，value=dict_value（或直接存 JSON 列表）。P1 用 Hash 存 dict_code→完整字典项 JSON。

---

## 八、菜单管理设计

- system_menu CRUD（树形查询 by parent_code）
- 四端可见性：domain_type 过滤（admin/channel/agent/client）
- 角色-菜单关联（organ_role_menu_rel）批量授权

---

## 九、P1 验收标准

| 维度 | 标准 |
|------|------|
| RBAC | 超管 admin 登录后 `@SaCheckPermission("organ:account:list")` 通过；普通账号无权限抛 10200 |
| 状态机 | 应用启动后 Redis `dayan:sm:rule:EQUITY_SM` 含 12 条规则；`engine.transition("EQUITY_SM",0,"outbound")` 返回 1 |
| 组织架构 | organ_info/department/employee CRUD + 树形查询 + ancestors 维护 |
| 账号管理 | organ_account CRUD + 登录失败 5 次锁定 |
| 操作日志 | 标注 `@OperationLog` 的接口调用后 system_operation_log 有记录 |
| 字典缓存 | DictService.getByType 命中 Redis；DB 变更后缓存刷新 |
| 菜单管理 | 树形 CRUD + 按角色查询菜单 |
| 单元测试 | RBAC/状态机/组织树/账号锁定 核心逻辑有测试覆盖 |

---

## 十、任务拆分与执行顺序

| 任务 | 内容 | 依赖 |
|------|------|------|
| P1-A | RBAC：permission/role/role_permission_ship/account_role_rel 的 Service+Controller + DayanStpInterface | P0 |
| P1-B | 状态机：SystemStateRuleLoader + 启动预热 | P0 |
| P1-C | 组织架构：organ_info/department/employee Service+Controller | P0 |
| P1-D | 账号管理：organ_account CRUD + 登录失败锁定 | P1-A（角色关联） |
| P1-E | 操作日志：SystemOperationLogPublisher 落库 | P0 |
| P1-F | 字典缓存 + 菜单管理 + 系统配置 CRUD | P0 |

执行顺序：P1-B（状态机，最快）→ P1-E（操作日志，最快）→ P1-A（RBAC 核心）→ P1-C（组织）→ P1-D（账号）→ P1-F（字典/菜单/配置）。
