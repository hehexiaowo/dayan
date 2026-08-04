# P3 养老机构域 + 供应商域 + 分销商域 - 设计规格

> **版本**：v1.0
> **编制日期**：2026-08-04
> **适用范围**：大雁养老 P3 阶段（park/supplier/distributor 域，后端核心优先）
> **依据**：`docs/08项目计划书.md` §2.3 P3、P0/P1/P2 已完成基础
> **前置**：P0（41 模块骨架 + 127 表 DDL + 状态机引擎 + 编码生成器）、P1（RBAC + 组织 + 系统）、P2（渠道/代理人/客户）

---

## 一、范围（后端核心优先）

三域共 26 张表，P3 聚焦资源供给端核心 CRUD + 机构状态机 + 供应商审核入驻：

| 序号 | 域 | 表数 | 核心内容 |
|------|----|----|---------|
| 1 | **养老机构域（park_）** | 15 | park_info 机构主信息（4 态状态机） / park_media_*（4 表） / park_facility 设施 / park_service_item 服务 / park_adviser 顾问 / park_periphery 周边 / park_room_type + price 房型定价 / park_care_type + price 照护定价 / park_food_type + price 餐饮定价 |
| 2 | **供应商域（supplier_）** | 10 | supplier_info 供应商（审核流） / supplier_account 账号 / supplier_contract 合同（续约链） / supplier_evaluation 季度评估 / supplier_contact 联系人 / supplier_role/permission RBAC / supplier_open_platform 开放平台 |
| 3 | **分销商域（distributor_）** | 1 | distributor_info 分销商（企业/个人，渠道归属） |

### P3 本阶段（必做）
- 三域核心 Entity 的 Service + Controller CRUD
- park_info 4 态状态机接入（待审核→已上线→已下架/暂停营业）
- supplier_info 审核流程（待审核→已通过/已驳回）+ 信用代码唯一校验
- supplier_contract 续约链管理（parent_contract_code）+ 合同编号唯一 + 日期校验
- supplier_evaluation 4 维评分 + 总分计算 + A/B/C/D 评级
- supplier_account 多账号管理（isAdmin 标记主账号）
- supplier_contact 主联系人唯一性（同供应商仅 1 个 isPrimary=1）
- park_room_type 总数≥可用数校验
- distributor_info 编码 DS+5 位 + 企业/个人两种类型

### P3 后置（不在本阶段）
- supplier_open_platform 真实 OAuth/签名验证（P3 只存配置）
- 供应商/分销商前端页面（P8 Admin 前端阶段）
- 机构多媒体文件实际上传（P3 只存 URL，OSS 接入 P8+）
- 机构 VR 三种格式解析（P3 仅存 url+format 字段）
- 房型/照护/餐饮时段冲突的强校验（P3 做基础日期校验，复杂时段交叉检测 P5+）

---

## 二、全局约束（实现者必读）

### 2.1 包结构与命名
- **park 域**：`com.dayan.park.{controller,service,dto,vo,converter,statemachine}`
- **supplier 域**：`com.dayan.supplier.{controller,service,dto,vo,converter,statemachine}`
- **distributor 域**：`com.dayan.distributor.{controller,service,dto,vo,converter}`
- Controller 分端包：`controller/admin`（Admin 后台管理）、`controller/supplier`（供应商端，预留）。P3 只实现 `admin` 端 Controller。
- Service 接口在 `service/`，实现类在 `service/impl/`，命名 `XxxService` / `XxxServiceImpl`。
- DTO 命名：`XxxCreateDTO` / `XxxUpdateDTO` / `XxxQueryDTO`；VO 命名：`XxxVO`。

### 2.2 编码生成（CodeGenerator Bean）
- park_info：前缀 `PK` + 5 位（`BusinessCode.PARK`）
- supplier_info：前缀 `SP` + 5 位（`BusinessCode.SUPPLIER`）
- distributor_info：前缀 `DS` + 5 位（`BusinessCode.DISTRIBUTOR`）
- supplier_contract：前缀 `HT` + 5 位（直接用字面量 `"HT"`，BusinessCode 无 CONTRACT 常量）
- 其余子表（media/facility/service 等）使用数据库 AUTO_INCREMENT 主键，无需业务编码。
- **CodeGenerator 用法（与 channel/organ 域一致）**：
  - `CodeGenerator` 已是全局 Spring Bean（由 dayan-module-channel 或 dayan-module-organ 的 `@ConditionalOnMissingBean(CodeGenerator.class)` 装配，容器中只存在一个）。
  - 各模块 Service 通过 `@RequiredArgsConstructor` 注入 `private final CodeGenerator codeGenerator;`
  - 调用：`String code = codeGenerator.generate(BusinessCode.PARK);`（全局唯一）或 `codeGenerator.generate("HT");`
  - **若某模块需确保 CodeGenerator Bean 存在**：在其 `config/` 下建 `XxxCodeGeneratorConfig.java`（参考 `ChannelCodeGeneratorConfig`），用 `@ConditionalOnMissingBean(CodeGenerator.class)` 防重复。但鉴于 channel/organ 已装配，**park/supplier/distributor 模块通常无需再建此配置**，直接注入即可。
  - **重要**：各 Service 只注入 `CodeGenerator`，不要新建实例、不要建静态 register。

### 2.3 租户隔离规则
- **park_* / supplier_* / distributor_* 均为平台共享表**（DayanTenantHandler 已忽略 organ_/park_/supplier_/distributor_/butler_ 前缀），**不参与 channel_code 字段隔离**。
- 这些表的查询/写入**不带 channel_code 条件**，直接按 park_code/supplier_code/distributor_code 维度操作。

### 2.4 状态机（park_info）
- bizType = `park_info`（已存在于 state_machine_rule 种子数据）
- 4 态：`0=pending_audit`（待审核）→ `1=online`（已上线）→ `2=offline`（已下架）/ `3=suspended`（暂停营业）
- 流转事件：`approve`（0→1 上线）、`offline`（1→2 下架）、`online`（2→1 重新上线）、`suspend`（1→3 暂停）、`resume`（3→1 恢复）
- **状态字段裁定（重要）**：park_info 表用 `operate_status` 字段承载状态机 4 态（扩展取值语义，不改 DDL 结构）：
  - `operate_status` 取值：`0=待审核 / 1=已上线 / 2=已下架 / 3=暂停营业`（覆盖原注释"1=开业/2=停业"）
  - `is_published` 字段同步维护：`operate_status==1` 时 `is_published=1`（对外可见），其余 `is_published=0`
  - 状态机引擎调用：`int to = stateMachineEngine.transition("PARK_SM", currentOperateStatus, event);`
  - domain 参数恒为 `"PARK_SM"`（machineCode）
- Controller 状态转移接口：`POST /admin-api/park/info/transition`（传 parkCode + event），service 内部读取当前 operate_status → 调引擎 → 落库 operate_status + 联动 is_published
- 状态机规则种子已存在于 `db/migration/seed/state_machine_seed.sql`（PARK_SM，5 条规则），P3 无需新增种子。

### 2.5 供应商审核流程（supplier_info）
- status 字段：1=待审核、2=已通过、3=已驳回（与 auditStatus 配合）
- Admin 审核：待审核→通过（status=2）/→驳回（status=3，填 auditRemark）
- 信用代码（unifiedCreditCode）唯一校验（同类型供应商内）

### 2.6 统一返回与分页
- 所有 Controller 返回 `R<T>` 或 `R<PageResult<T>>`（`com.dayan.common.core.resp`）
- 分页用 `PageResult`（service 返回 `Page<XX>` → 转 `PageResult<XXVO>`）
- 异常抛 `BusinessException`（`com.dayan.common.core.exception`），用 `ErrorCode` 错误码

### 2.7 依赖注入风格
- Service 实现类用 `@RequiredArgsConstructor` + `private final XxxMapper xxxMapper;`
- DTO→Entity 用 MapStruct converter（`@Mapper(componentModel = "spring")`），或手动 `BeanUtils.copyProperties`。P3 沿用 P2 模式：**手动 copyProperties**（保持与现有 channel/agent/client 模块一致，无 MapStruct 复杂配置）。

### 2.8 参考实现（P2 已落地的模式，务必复用）
- `dayan-module-channel` 的 Service+Controller 结构、DTO/VO 命名、tree()、codeGenerator 用法、config 注册
- `dayan-module-organ` 的 `DayanStpInterface`（P3 supplier 端 RBAC 可复用模式，但本阶段不实现）
- `dayan-module-system` 的 `SystemStateRuleLoader` + `StateMachineWarmUpRunner`（park 状态机接入参考）

---

## 三、供应商域设计（10 表）

### 3.1 supplier_info（供应商主信息）
- CRUD + 审核流（status: 1待审/2通过/3驳回）
- 编码 SP+5（CodeGenerator）
- unifiedCreditCode 唯一校验（同 supplierType 内）
- supplierType: 1=养老机构供应商 / 2=商品供应商 / 3=服务供应商
- 审核接口：`POST /admin-api/supplier/info/audit`（传 supplierCode + auditStatus + auditRemark）

### 3.2 supplier_account（供应商账号）
- 多账号管理，isAdmin=1 主账号
- P0 已有 SupplierAuthService 登录骨架（本阶段补 CRUD）
- 密码 BCrypt（复用 `common-security.password.PasswordService`）

### 3.3 supplier_contract（合同，续约链）
- 4 类合同（contractType: 1=机构入驻/2=商品供应/3=服务供应/4=渠道合作）
- 4 种结算周期（settlementCycle: 1=月结/2=季结/3=半年结/4=年结）
- 续约链：parent_contract_code 指向原合同，renewCount 记录续约次数
- contractCode 唯一校验（HT+5 位，`codeGenerator.generate("HT")`）
- 日期校验：effectiveDate < expireDate

### 3.4 supplier_evaluation（季度评估）
- 4 维评分：serviceQualityScore / facilityQualityScore / cooperationScore / complaintRate（投诉率%）
- totalScore 自动计算：`(service + facility + cooperation) / 3 * (1 - complaintRate/100)`（保留 2 位小数）
- scoreLevel: 1=A(≥90) / 2=B(80-89) / 3=C(70-79) / 4=D(<70)
- evalPeriod 格式：`YYYYQN`（如 2026Q3）

### 3.5 supplier_contact（联系人）
- 4 类型（contactType: 1=商务/2=财务/3=运营/4=其他）
- 主联系人：同 supplierCode 下 isPrimary=1 仅 1 个（设主时其余置 0）

### 3.6 supplier_role / permission / account_role_rel / role_permission_ship
- P3 仅 CRUD 框架（RBAC 查询逻辑后置，与 channel 域 P2 模式一致）

### 3.7 supplier_open_platform
- app_key/app_secret 生成 + AES-256-GCM 加密（复用 `AesGcmUtil`，参考 ChannelOpenPlatform 实现）

---

## 四、养老机构域设计（15 表）

### 4.1 park_info（机构主信息，状态机核心）
- CRUD + 状态机 4 态流转
- 编码 PK+5（CodeGenerator）
- supplierCode 关联校验（供应商须存在且 status=2 已通过）
- abilityType 9 类机构能力 / natureType 6 类性质 / dayanLevel 6 级（S/A/B/C/D/E）
- 坐标校验：longitude/latitude 非空时格式合法（经度 -180~180，纬度 -90~90，字符串校验）
- 状态机接口：`POST /admin-api/park/info/transition`（传 parkCode + event）

### 4.2 park_media_*（4 表：image/video/file/vr）
- 按 parkCode 维度 CRUD
- 图片 11 类分类（imageType）
- VR 三种格式（vrFormat: 1=全景图/2=3D模型/3=视频）
- URL 唯一校验（同 parkCode + 同类型下 url 不重复）

### 4.3 park_facility / park_service_item / park_adviser / park_periphery
- 各自按 parkCode 维度 CRUD
- park_adviser: isPrimary 主顾问唯一（同 parkCode 仅 1 个）

### 4.4 park_room_type + park_room_price（房型定价）
- room_type: 5 类房型（roomCategory）+ 配套标记（hasBathroom 等）
- 总数校验：`totalRooms >= availableRooms`
- room_price: 时段定价（priceType: 1=月/2=季/3=年/4=临时），effectiveDate/expireDate 日期校验，isCurrent=1 当前价唯一（同 roomTypeCode 下）

### 4.5 park_care_type + park_care_price（照护定价）
- care_type: 5 级照护（careLevel 1-5）
- care_price: 同 room_price 模式

### 4.6 park_food_type + park_food_price（餐饮定价）
- food_type: mealPlan（1=三餐/2=三餐+点心/3=定制）
- food_price: 同 room_price 模式

---

## 五、分销商域设计（1 表）

### 5.1 distributor_info（分销商信息）
- CRUD
- 编码 DS+5（CodeGenerator）
- subjectType: 1=企业 / 2=个人
- 企业：unifiedCreditCode + legalPerson + businessLicenseNo
- 个人：idCard + gender + phone
- **渠道归属 channel_code、分佣比例 commission_rate**：规格初稿依据 PRD 提出，但 `db/migration/17_distributor.sql`（P0 已执行验证）与 `DistributorInfo` Entity **均无此两列**——DB 设计文档 §3.17 未落地。**裁定：P3 遵循现有 DDL，不实现此两字段及其校验；待后续阶段若 PRD 确需，走表结构变更（DDL 补列 + Entity 补字段）后再补。**

---

## 六、API 路径规范

所有 P3 接口走 Admin 端（本阶段）：

| 域 | 前缀 | 示例 |
|----|------|------|
| 供应商 | `/admin-api/supplier/info` `/admin-api/supplier/account` `/admin-api/supplier/contract` `/admin-api/supplier/evaluation` `/admin-api/supplier/contact` `/admin-api/supplier/role` 等 | `GET /admin-api/supplier/info/page` |
| 机构 | `/admin-api/park/info` `/admin-api/park/media-image` `/admin-api/park/facility` ... `/admin-api/park/room-type` `/admin-api/park/room-price` 等 | `POST /admin-api/park/info/transition` |
| 分销商 | `/admin-api/distributor/info` | `GET /admin-api/distributor/info/page` |

标准动作：`/page`（分页查）、`/list`（全量查）、`/{code}`（详情）、`POST /`（新增）、`PUT /`（更新）、`DELETE /{code}`（删除）。
特殊动作：供应商审核 `/audit`、机构状态机 `/transition`、评估总分计算 `/calc-total`。

---

## 七、P3 验收标准

| 维度 | 标准 |
|------|------|
| 供应商 CRUD | supplier_info 审核流 + 账号 + 合同（续约链）+ 评估（4 维评分+评级）+ 联系人（主联系人唯一） |
| 机构 CRUD | park_info 状态机 4 态 + media 4 表 + facility/service/adviser/periphery + room/care/food type+price 各 2 表 |
| 分销商 CRUD | distributor_info 企业/个人双类型 |
| 编码生成 | PK/SP/DS/HT 编码正确生成且唯一 |
| 编译 | 41 模块全量编译通过 |

---

## 八、任务拆分与执行

三域在不同模块（park/supplier/distributor），互不冲突，可三路并行。

| 任务 | 内容 | 可并行 |
|------|------|--------|
| **P3-A** | 供应商域 Service+Controller（10 表） | ✅ |
| **P3-B** | 养老机构域 Service+Controller（15 表）+ park 状态机接入 | ✅ |
| **P3-C** | 分销商域 Service+Controller（1 表） | ✅ |

分派策略：三个子智能体并行实现，各自独立提交；完成后做全量编译验证。
