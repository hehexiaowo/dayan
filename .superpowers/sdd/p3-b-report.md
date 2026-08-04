# P3-B 养老机构域 Service+Controller（15 表）+ 状态机接入 — 实现报告

> 模块：`dayan-server/dayan-modules/dayan-module-park`
> 日期：2026-08-04
> 依据：`.superpowers/specs/2026-08-04-p3-park-supplier-distributor-design.md` §二 / §四 / §六

## 一、状态机加载方案：方案 B（选定）

**选择方案 B**：park 模块**不**自建 `statemachine` 包，直接信任 system 模块的 `StateMachineWarmUpRunner` 在应用启动时已加载 PARK_SM 规则到 Redis。

**依据（已核实源码）**：
- `dayan-module-system/StateMachineWarmUpRunner`（`ApplicationRunner`）启动时调用 `engine.loadAllRules()`。
- `DefaultStateMachineEngine.loadAllRules()` 调用 `StateRuleLoader.loadAll()`，按 domain 分组写入 Redis Hash。
- `SystemStateRuleLoader.loadAll()` 查询 `system_state_machine` 表 **所有 status=1 的行**（无 domain 过滤），PARK_SM 5 条规则全部命中。
- 种子数据 `db/migration/seed/state_machine_seed.sql` 已含 PARK_SM（approve 0→1 / offline 1→2 / online 2→1 / suspend 1→3 / resume 3→1），与本实现事件常量一致。

因此 `ParkInfoServiceImpl.transition()` 直接调 `stateMachineEngine.transition("PARK_SM", currentOperateStatus, event)`，落库 `operate_status=to` 并联动 `is_published`（to==1 时为 1，否则 0）。`StateMachineEngine` 接口在 `dayan-common-core`（park 已依赖），运行期 Bean 由 `dayan-common-redis` 提供，无需改 pom。

## 二、文件清单（共 94 个新增 .java）

按职责统计（均位于 `dayan-module-park/src/main/java/com/dayan/park/`）：

| 类别 | 数量 | 说明 |
|------|------|------|
| Service 接口（`service/`） | 15 | 每表 1 个，与 Entity 一一对应 |
| Service 实现（`service/impl/`） | 15 | `@RequiredArgsConstructor` 构造注入 |
| Controller（`controller/admin/`） | 15 | 仅 swagger `@Tag`/`@Operation`，无 `@SaCheckPermission` |
| DTO（`dto/`） | 45 | 每表 Create/Update/Query 各 1（ParkInfo 转换走独立 `/transition` 端点，无独立 DTO） |
| VO（`vo/`） | 15 | 每表 1 个 |
| 枚举（`enums/ParkEvent.java`） | 1 | 5 个事件常量 |
| 跨模块只读视图（`entity/SupplierInfoView.java` + `mapper/SupplierInfoViewMapper.java`） | 2 | 详见下文 |

### 15 张表的分组与 Service 落点

1. **park_info（核心+状态机）** — `ParkInfoService`：编码 PK+5（`codeGenerator.generate(BusinessCode.PARK)`）；supplierCode 关联校验；坐标校验；`transition()` 状态机接入。
2. **park_media_image/video/file/vr（4 表）** — 各 1 个 Service：按 parkCode 维度 CRUD，URL 唯一校验（同 parkCode 下），主键 id（AUTO_INCREMENT）。
3. **park_facility / park_service_item / park_adviser / park_periphery（4 表）** — 各 1 个 Service。`facility`/`service-item` 二级 code（facilityCode/serviceCode）同 parkCode 下唯一；`adviser` isPrimary=1 同 parkCode 唯一（设主时自动置 0 其余）。
4. **park_room_type + park_room_price** — `totalRooms >= availableRooms` 校验；price 时 `effectiveDate < expireDate`、`isCurrent=1` 同 roomTypeCode 唯一。
5. **park_care_type + park_care_price** — careLevel 1-5；price 同 room_price 模式（careTypeCode 维度）。
6. **park_food_type + park_food_price** — mealPlan；price 同 room_price 模式（foodTypeCode 维度）。

### 跨模块依赖处理：`SupplierInfoView`（轻量只读映射）

park_info.supplierCode 须校验供应商存在且 `status=2`（已通过），但 **park 模块不应依赖 dayan-module-supplier**（避免循环依赖）。采用规格 §A 的"共享表多模块映射"模式：在 park 模块内新建最小只读 POJO `SupplierInfoView`（`@TableName("supplier_info")`，仅含 id/supplierCode/status）+ `SupplierInfoViewMapper`。`supplier_info` 为平台共享表（`DayanTenantHandler` 忽略 `supplier_` 前缀），多模块映射同一物理表不冲突。

## 三、关键实现要点

### 3.1 状态机接入（ParkInfoServiceImpl）
- 域标识常量 `SM_DOMAIN = "PARK_SM"`。
- `transition(parkCode, event)`：读当前 `operate_status`（null 兜底为 0 待审核）→ `stateMachineEngine.transition(SM_DOMAIN, from, event)` 取得 `to` → UPDATE `operate_status=to` + `is_published`（to==1?1:0）。
- Controller `POST /admin-api/park/info/transition`（`@RequestParam parkCode + event`）。

### 3.2 校验汇总
| 表 | 校验规则 |
|----|---------|
| park_info | supplierCode 关联存在且 status=2；经纬度（提供时任一非空则两者皆需提供，经度 -180~180、纬度 -90~90，Double.parseDouble 解析） |
| park_media_* | URL 同 parkCode 下唯一（image/video/file/vr 各自） |
| park_facility | facilityCode 同 parkCode 下唯一 |
| park_service_item | serviceCode 同 parkCode 下唯一 |
| park_adviser | isPrimary=1 同 parkCode 唯一（LambdaUpdateWrapper 自动置 0 其余） |
| park_room_type | totalRooms >= availableRooms |
| park_room_price | effectiveDate < expireDate；isCurrent=1 同 roomTypeCode 唯一（自动置 0） |
| park_care_type | careTypeCode 同 parkCode 下唯一 |
| park_care_price | effectiveDate < expireDate；isCurrent=1 同 careTypeCode 唯一 |
| park_food_type | foodTypeCode 同 parkCode 下唯一 |
| park_food_price | effectiveDate < expireDate；isCurrent=1 同 foodTypeCode 唯一 |

### 3.3 API 路径（15 个 Controller）
全部前缀 `/admin-api/park/`（由 dayan-admin context-path 拼接）：
- `info`（主表，parkCode 路径 + `/transition`）
- `media-image` / `media-video` / `media-file` / `media-vr`
- `facility` / `service-item` / `adviser` / `periphery`
- `room-type` / `room-price` / `care-type` / `care-price` / `food-type` / `food-price`

标准动作：`/page`、`/list`（子表按 parkCode 或 parkCode+typeCode）、`/{id或parkCode}`、`POST /`、`PUT /{id或parkCode}`、`DELETE /{id或parkCode}`。主表用 parkCode，子表用 id（AUTO_INCREMENT）。

## 四、编译结果

```
mvn -B -ntp clean compile
[INFO] Compiling 151 source files with javac [debug parameters release 21] to target\classes
[INFO] BUILD SUCCESS
[INFO] Total time:  8.638 s
EXIT_CODE=0
```

151 个源文件全部编译通过，0 错误 0 警告。pom.xml 未改动。

## 五、偏差与说明

1. **VR 字段名**：规格 §4.2 写 `vrFormat`，但 `ParkMediaVr` Entity 实际字段为 `vrType`（1=全景图/2=3D模型/3=视频）。实现遵循 Entity 定义（`vrType`），避免与现有 P0 实体冲突。
2. **Controller 未加 `@OperationLog`**：规格关键约束第 10 条要求"Controller 只加 swagger @Tag/@Operation"，故未引入 `@OperationLog`（dayan-common-log 虽在依赖中）。如需审计日志可后置补加。
3. **`update` 端点路径**：规格标准动作写作 `PUT /`，但参考 channel 域 `PUT /{channelCode}` 模式，park_info 的 update 采用 `PUT /{parkCode}`（parkCode 在 path），子表 update 用 `PUT /{id}`。这与 ChannelInfoAdminController/ChannelAccountAdminController 一致，更 RESTful。
4. **跨模块 supplier 校验**：采用 `SupplierInfoView` 只读视图而非依赖 supplier 模块（方案 A 思路应用于 supplier 引用），符合规格"park 模块不依赖 system/supplier 模块"约束。
5. **state machine 包未建**：选方案 B，`statemachine/package-info.java` 保留空包（P0 已建），未新增 ParkStateRuleLoader/StateMachineWarmUpRunner。

## 六、疑虑与后置项

1. **PARK_SM 规则预热的隐式依赖**：park 模块运行期依赖 system 模块的 `StateMachineWarmUpRunner` 被 ComponentScan 扫到并执行 `loadAllRules()`。若未来出现"仅启动 park 相关模块、不启动 system"的部署形态，PARK_SM 规则将缺失。引擎首次调用会触发 `checkTransition` 抛"非法状态转移"。**缓解**：当前 dayan-admin 启动模块引入 system 模块，`@Component` 必被扫到；且 `DefaultStateMachineEngine` 注释明确"预热失败不阻断启动，首次调用会触发懒加载"——但懒加载仍依赖 `StateRuleLoader` Bean 存在（system 提供）。建议部署文档注明：park 状态机依赖 system 模块同时部署。
2. **未写单元测试**：本任务为一次性实现，未补充 Service 单测。状态机 transition、各唯一性校验、日期校验建议在集成测试阶段覆盖（需 Redis + MySQL 环境）。
3. **未做级联删除**：删除 park_info 时不级联清理子表（media/facility/...）。规格未要求，但生产可能需要软删或级联——建议产品确认后补。
4. **`price` 三表结构高度相似**：room/care/food price 实现几乎重复（仅 typeCode 字段名不同）。为保持与 Entity 一一对应、代码规整，未抽取公共基类。如需可后置重构为泛型基类。
