# P5 服务域 + 管家域 - 设计规格

> **版本**：v1.0
> **编制日期**：2026-08-04
> **适用范围**：大雁养老 P5 阶段（service 域 7 表 + butler 域 8 表）
> **依据**：`docs/08项目计划书.md` §2.3 P5、P0-P4 已完成基础
> **前置**：P4 权益域（权益激活触发会话创建）、P1 状态机引擎、P2 客户域

---

## 一、范围

管家服务四环节闭环（需求收集→方案定制→全程安排→回访品控），共 15 表：

| 序号 | 域 | 表数 | 核心 |
|------|----|----|------|
| 1 | **服务域 service_** | 7 | service_session 会话（7态状态机+子状态） / demand 需求 / solution 方案 / arrange 安排 / followup 回访 / evaluation 评价 / visit_record 探访 |
| 2 | **管家域 butler_** | 8 | butler_info 管家 / account 账号 / client_rel 客户绑定 / service_record 服务记录 / rating 评价 / schedule 排班 / skill 技能 / account_role_rel |

### P5 本阶段（必做）
- 服务域 7 表 Service+Controller + SERVICE_SESSION_SM 状态机
- 管家域 8 表 Service+Controller
- 服务四环节闭环：会话创建→需求收集→方案定制→安排→服务→回访→完成
- 会话子状态管理（sub_status: normal/hold/urgent/reassign/refund_review/refund_done/interrupted）
- 权益激活触发会话创建（跨模块：equity 调 service）
- 管家分配（一客户一管家约束）
- 服务评价（一会话一评价）
- SLA 超时升级定时任务（2小时未受理→urgent）
- 7天回访定时任务

### P5 后置（不在本阶段）
- **service_change_log 变更日志**：计划书提到但 DDL/模块均无此表，**裁定后置**（无表无法实现，待表结构补充）
- Admin 前端页面（P8）
- 排班冲突强校验的复杂算法（P5 做基础时间重叠检测）
- 退款审批流（refund_review/refund_done 子状态仅记录，审批流 P7 结算域）

---

## 二、全局约束（实现者必读）

### 2.1 包结构
- 服务域：`com.dayan.service.{controller.admin,service,service.impl,dto,vo,enums}`
- 管家域：`com.dayan.butler.{controller.admin,service,service.impl,dto,vo,enums}`
- Controller 仅 `controller/admin`（Admin 端管理）

### 2.2 分片表主键改造（P5 必须执行）
同 P4 裁定，分片表 DDL 无 AUTO_INCREMENT，Entity 的 `IdType.AUTO` 改为 `IdType.ASSIGN_ID`：
- **service 域全 7 表**（全是分片表）：ServiceSession/ServiceEquityDemand/ServiceEquitySolution/ServiceEquityArrange/ServiceEquityFollowup/ServiceEvaluation/ServiceVisitRecord
- **butler 域 4 张分片表**：ButlerClientRel/ButlerServiceRecord/ButlerRating/ButlerSchedule
- butler 域 4 张平台共享表保持 AUTO：ButlerInfo/ButlerAccount/ButlerAccountRoleRel/ButlerSkill（DDL 有 AUTO_INCREMENT）

### 2.3 租户隔离（P5 必须执行）
- **service_ 加入 DayanTenantHandler DEFAULT_IGNORE_PREFIXES**（同 equity_ 理由：Admin 全局视图，服务跨渠道分配管家）
  - 改 `dayan-common-mybatis/.../DayanTenantHandler.java`，数组追加 `"service_"`
- **butler_ 已在忽略清单** ✓（P0 已配），无需改

### 2.4 跨模块依赖（关键裁定）
服务域需要引用 equity/butler/park/client 的数据，但**模块间不直接依赖**（避免循环、保持解耦，P3 park 已建立 `SupplierInfoView` 只读映射模式）。裁定：

- **service 模块不依赖 equity/butler/park/client 模块**。
- 需要跨模块读数据时，在 service 模块建只读 View Entity + ViewMapper（`@TableName` 指向目标表），例如：
  - `entity/EquityDepotView.java`（`@TableName("equity_depot")`，只含需要的字段）+ `mapper/EquityDepotViewMapper.java`——查权益信息
  - `entity/ButlerInfoView.java`（`@TableName("butler_info")`）+ mapper——查管家信息
  - `entity/ParkInfoView.java`（`@TableName("park_info")`）+ mapper——查机构推荐
  - `entity/ClientInfoView.java`（`@TableName("client_info")`）+ mapper——查客户信息
- **权益激活触发会话创建**：方向是 equity→service，但 equity 模块也不应依赖 service 模块。**裁定**：不在 equity 激活时直接调 service，改为：
  - 方案 A（推荐）：service 模块提供"按 equity_code 创建会话"的 Admin 接口，由 Admin 手动或前端在激活后调用。
  - 方案 B：用 Spring 事件（equity 发 `EquityActivatedEvent`，service 监听）——但跨模块事件需共享事件类，放 common-core。**P5 用方案 A**（最简，不引入事件机制复杂度），在规格里注明"权益激活后由调用方显式创建会话"。

### 2.5 编码生成
- session_code：`"SS" + String.format("%010d", sequenceProvider.next("code:seq:SS:0"))`（BusinessCode.SERVICE_SESSION="SS"）
- demand/solution/arrange/followup 的业务编码（如有 code 字段）：`"DM"/"SO"/"AR"/"FU" + format(...)`，各自序列
- butler_code：`"BT" + String.format("%05d", sequenceProvider.next("code:seq:BT:0"))`（BusinessCode.BUTLER="BT"）
- 注入 `private final SequenceProvider sequenceProvider;`

### 2.6 状态机（SERVICE_SESSION_SM，7 态）
- domain 参数 = `"SERVICE_SESSION_SM"`
- 状态字段 = service_session.`session_status`，7 态：`1=待分配 / 2=待收集 / 3=方案中 / 4=安排中 / 5=服务中 / 6=已完成 / 7=已取消`
  - 注：DDL 注释写"2=处理中,3=方案待确认,4=服务安排中"，状态机种子写"2=待收集,3=方案中,4=安排中"——**以状态机种子的数值流转为准**（描述仅注释差异）
- 9 条规则（已存在于种子）：
  - `assign_butler`: 1→2（分配管家）
  - `submit_demand`: 2→3（提交需求，进入方案）
  - `confirm_solution`: 3→4（确认方案）
  - `reject_solution`: 3→2（驳回方案，退回需求）
  - `start_service`: 4→5（开始服务）
  - `finish`: 5→6（完成）
  - `cancel`: 1→7 / 2→7 / 5→7（取消，仅特定态可取消）
- 引擎调用：`stateMachineEngine.transition("SERVICE_SESSION_SM", from, event)`
- 注入 `private final StateMachineEngine stateMachineEngine;`

### 2.7 子状态（sub_status，独立于状态机）
session_status 走状态机；sub_status 是状态内的附属标记，由应用层直接修改（不经状态机），7 值：
- `normal`（默认）/ `hold`（暂停）/ `urgent`（紧急，SLA 超时升级）/ `reassign`（改派管家）/ `refund_review`（退款审核中）/ `refund_done`（退款完成）/ `interrupted`（中断）
- 终态校验：session_status=6(完成)/7(取消) 时，sub_status=refund_done 不可再转
- SLA 超时：session_status=1(待分配) 且 2小时未受理 → sub_status=urgent（定时任务）

### 2.8 统一返回 / 异常 / DTO
- `R<T>` / `R<PageResult<T>>`，`BusinessException`+`ErrorCode`
- DTO→Entity→VO 手动 `BeanUtils.copyProperties`
- Service `@RequiredArgsConstructor`，核心操作 `@Transactional`

---

## 三、服务域设计（7 表）

### 3.1 service_session（核心：服务会话）
- CRUD + 状态机 7 态流转
- session_code(SS+10) 唯一
- 创建会话接口：`POST /service/session`（传 equityCode/clientCode/serviceType/...），初始 session_status=1, sub_status=normal
- 状态转移接口：`POST /service/session/transition`（传 sessionCode + event）
- 分配管家：`POST /service/session/assign-butler`（传 sessionCode + butlerCode）→ event=assign_butler(1→2)，写 butlerCode/butlerFullName/acceptTime
- 各业务动作接口（submit-demand/confirm-solution/reject-solution/start-service/finish/cancel）封装对应 event

### 3.2 service_equity_demand（需求收集）
- 按 sessionCode CRUD
- demandType: 5 类需求（1机构入住/2机构参观/3场景活动/4居家护理/5健康咨询）
- budgetMin/budgetMax 预算范围，expectedTime 时间要求
- demandCode(DM+序列)
- 提交需求时联动会话状态：submit_demand(2→3)
- **关联健康档案/照护需求**：计划书要求联动 client_health_profile/client_care_need，但 service 模块不依赖 client 模块。**裁定**：P5 仅在 demand 的 healthSummary/careLevelNeed 字段记录（文本），不跨模块更新 client 表；联动后置。

### 3.3 service_equity_solution（方案定制）
- 按 sessionCode CRUD，多方案（solutionType: 1推荐/2备选）
- recommendedParks（JSON 数组，推荐机构 parkCode 列表）
- estimatedCost 价格预估
- 至少 1 方案校验（确认方案时须存在 isAccepted=1 的方案）
- solutionCode(SO+序列)，adjustCount 版本号
- 确认方案：confirm_solution(3→4)；驳回：reject_solution(3→2)，方案 adjustCount+1

### 3.4 service_equity_arrange（全程安排）
- 按 sessionCode/solutionCode CRUD
- arrangeType: 4 类（1待入住/2待参访/3服务中/4已完成）—— 注：这是 arrange 自身的 status 语义
- arrangeCode(AR+序列)
- isConfirmed 确认标记（安排确认后方可 start_service）
- 时间安排：arrangeDate + arrangeTimeStart/End

### 3.5 service_equity_followup（回访品控）
- 按 sessionCode/arrangeCode CRUD
- followupCode(FU+序列)
- 4 维满意度（serviceSatisfaction/parkSatisfaction/butlerSatisfaction/overallSatisfaction，1-5）
- isFollowupNeeded + nextFollowupDate（评分<3 需跟进）
- 7 天内必回访（定时任务校验：完成后 7 天内须有 followup 记录）

### 3.6 service_evaluation（服务评价）
- 按 sessionCode CRUD，**一会话一评价**（uk 或应用层校验 sessionCode 唯一）
- 4 维评分（attitude/professional/responsiveness/satisfaction，1-5）
- isAnonymous 匿名选项
- replyContent 回复

### 3.7 service_visit_record（探访记录）
- 按 butlerCode/parkCode CRUD
- overallScore 综合评分
- 6 项检查（facility/service/hygiene/food/safety，文本记录）

---

## 四、管家域设计（8 表）

### 4.1 butler_info（管家信息）
- CRUD + butlerCode(BT+5) 唯一
- butlerLevel 等级，organCode 所属组织
- status: 0停用/1启用

### 4.2 butler_account（管家账号）
- CRUD，多账号，密码 BCrypt（复用 PasswordService）

### 4.3 butler_client_rel（管家-客户绑定）
- 绑定/解绑
- **一客户一管家约束**：同 clientCode 仅 1 条 status=1 的有效绑定（绑定时校验，已有有效绑定则拒绝或先解绑旧绑定）
- status: 0已解绑/1有效

### 4.4 butler_service_record（管家服务记录）
- 按 butlerCode/clientCode CRUD
- communicateWay: 1电话/2企微/3微信/4上门/5其他
- serviceType 关联服务类型

### 4.5 butler_rating（管家评价）
- 按 butlerCode/clientCode CRUD
- rating 1-5，content 评价内容
- 关联 serviceRecordCode

### 4.6 butler_schedule（管家排班）
- 按 butlerCode/date CRUD
- scheduleType: 1上班/2休假/3外勤/4培训
- startTime/endTime
- **基础时间重叠检测**：同 butlerCode + 同 scheduleDate，新排班的时间段与已有排班不重叠（应用层校验）

### 4.7 butler_skill（管家技能）
- 按 butlerCode CRUD
- 技能标签/资质

### 4.8 butler_account_role_rel
- 账号-角色关联（P5 仅 CRUD 框架，RBAC 查询后置）

---

## 五、定时任务（dayan-job）

### 5.1 SLA 超时升级（新增 ServiceSlaScheduler）
- 新建 `dayan-job/.../scheduler/ServiceSlaScheduler.java`
- 每小时 `@Scheduled(cron = "0 0 * * * ?")`
- 扫描：session_status=1(待分配) 且 created_at < NOW()-2小时 且 sub_status='normal' → UPDATE sub_status='urgent'
- dayan-job 已依赖 service 模块？**未依赖**，需在 dayan-job/pom.xml 加 `dayan-module-service` 依赖，DayanJobApplication 的 @ComponentScan/@MapperScan 加 service 包

### 5.2 7天回访校验（合并到 ServiceSlaScheduler 或独立）
- 扫描：session_status=6(已完成) 且 complete_time < NOW()-7天 且无 followup 记录 → 记录告警日志（或 sub_status 标记）
- 简化：仅日志告警，不强制阻塞（回访缺失由运营跟进）

---

## 六、API 路径规范

| 子域 | 路径 |
|------|------|
| 会话 | `/service/session`（+ /transition /assign-butler /submit-demand /confirm-solution /reject-solution /start-service /finish /cancel） |
| 需求 | `/service/demand` |
| 方案 | `/service/solution` |
| 安排 | `/service/arrange` |
| 回访 | `/service/followup` |
| 评价 | `/service/evaluation` |
| 探访 | `/service/visit-record` |
| 管家 | `/butler/info` `/butler/account` `/butler/client-rel` `/butler/service-record` `/butler/rating` `/butler/schedule` `/butler/skill` |

标准动作：`/page /list /{code或id} POST / PUT / DELETE /{code或id}`。

---

## 七、验收标准

| 维度 | 标准 |
|------|------|
| 会话状态机 | 7 态 9 规则，合法/非法流转正确 |
| 四环节 | 会话→需求→方案→安排→服务→回访 闭环 |
| 子状态 | SLA 超时升级 urgent + 终态校验 |
| 管家 | 一客户一管家 + 排班时间重叠检测 |
| 评价 | 一会话一评价 |
| 定时任务 | SLA 升级 + 7天回访校验 |
| 分片表 | service 7表 + butler 4表 ASSIGN_ID |
| 编译 | 41 模块 BUILD SUCCESS |

---

## 八、任务拆分与执行

服务域强耦合（会话驱动四环节），管家域相对独立。拆分：

| 任务 | 内容 | 可并行 |
|------|------|--------|
| **P5-A** | 服务会话 + 四环节（session/demand/solution/arrange/followup 5 表）+ 状态机 + 分片表改造 + service_ 租户忽略 | 独立 |
| **P5-B** | 服务评价 + 探访记录（2 表） | P5-A 后（依赖 sessionCode） |
| **P5-C** | 管家域 8 表 + 一客户一管家 + 排班检测 | ✅ 与 P5-A/B 独立（不同模块） |
| **P5-D** | SLA 超时 + 7天回访定时任务（dayan-job） | P5-A 后 |

**执行**：P5-A 先做（核心，控制者亲自做基础设施改造）→ P5-B + P5-C 并行（不同模块无冲突）→ P5-D。P5-A 的基础设施（分片表+租户）由控制者预先做完，再分派子智能体。
