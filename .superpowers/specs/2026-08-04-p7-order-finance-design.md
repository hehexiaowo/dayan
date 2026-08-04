# P7 设计规格：订单域 + 结算域

> 阶段：P7（资金闭环核心）  
> 表数：订单域 4 表（order_equity/scene/course/sojourn）+ 结算域 7 表（finance_flow/bill/invoice/account/reconciliation/payment/refund）= **11 表**  
> DDL：`db/migration/15_order.sql`、`db/migration/16_finance.sql`  
> 参考实现：P4 权益域 `EquityDepotServiceImpl`（状态机+核心链路+@Transactional 模式）

---

## 0. 全局约束（实现者必读，逐字遵守）

### 0.1 主键策略（DDL → Entity 必须一致）

**11 张表全部为分片表**（DDL 均为 `id BIGINT NOT NULL` 无 `AUTO_INCREMENT`，注释"主键（雪花ID）"）：
- `order_equity` / `order_scene` / `order_course` / `order_sojourn`
- `finance_flow` / `finance_bill` / `finance_invoice` / `finance_account` / `finance_reconciliation` / `finance_payment` / `finance_refund`

**Entity 必须为**：
```java
/** 主键（分片表，雪花ID，MyBatis-Plus 自动分配） */
@TableId(type = IdType.ASSIGN_ID)
private Long id;
```
当前骨架 11 表 Entity 均为 `IdType.AUTO`（错误），**必须全部改为 `IdType.ASSIGN_ID`** 并改注释为"主键（分片表，雪花ID，MyBatis-Plus 自动分配）"。否则 insert 时 MyBatis-Plus 不分配 ID，MySQL 因列无默认值拒绝插入。

### 0.2 租户忽略（DayanTenantHandler）

`order_` / `finance_` 前缀**必须加入** `dayan-common-mybatis/.../tenant/DayanTenantHandler.java` 的 `DEFAULT_IGNORE_PREFIXES`。  
理由：订单/财务为平台全局资源，Admin 端需跨渠道全局查看；当 channel_code 为空时 `getTenantId()` 返回 `LongValue(0L)`，非忽略表会查不到数据。  
（当前清单已含 `system_/organ_/butler_/distributor_/equity_/service_/goods_/scene_/content_/course_`，追加 `order_`、`finance_`。）

### 0.3 状态机——ORDER_SM 种子需重写（关键冲突裁决）

**冲突**：现有 `db/migration/seed/state_machine_seed.sql` 的 ORDER_SM（8 态）状态模型为：
`0=待支付, 1=已支付, 2=处理中, 3=已完成, 4=已取消, 5=退款中, 6=已退款, 7=异常`

但 **DDL 注释与 PRD（docs/02 §2.3 ORDER_STATUS + §3.15）一致采用另一套 8 态**：
`0=待支付, 1=已支付, 2=部分发放, 3=已发放, 4=已完成, 5=已取消, 6=退款中, 7=已退款`

**裁决：以 DDL + PRD 为准（状态码是数据库已落地的契约），重写 ORDER_SM 种子。** 现有种子的"处理中/异常"语义不符合订单业务（订单履约=权益发放/场景排期/课程开通/旅居入住，用"部分发放/已发放"刻画）。

**重写后的 ORDER_SM（8 态，12 规则）**——状态码完全对齐 DDL `order_status` 注释：

| from | from_state_name | to | to_state_name | event_code | 语义 |
|------|-----------------|----|---------------|-----------|------|
| 0 | 待支付 | 1 | 已支付 | `pay` | 支付成功 |
| 0 | 待支付 | 5 | 已取消 | `cancel` | 用户取消或超时自动取消 |
| 1 | 已支付 | 2 | 部分发放 | `partial_deliver` | 部分履约（如批量权益部分出库） |
| 1 | 已支付 | 3 | 已发放 | `deliver` | 全部履约（权益全出库/排期确认/课程开通/旅居确认） |
| 2 | 部分发放 | 3 | 已发放 | `deliver` | 剩余部分发放完成 |
| 3 | 已发放 | 4 | 已完成 | `complete` | 业务完结（服务完成/离店/课程学完） |
| 1 | 已支付 | 6 | 退款中 | `refund_apply` | 申请退款 |
| 2 | 部分发放 | 6 | 退款中 | `refund_apply` | 部分发放后申请退款 |
| 3 | 已发放 | 6 | 退款中 | `refund_apply` | 已发放后申请退款 |
| 6 | 退款中 | 7 | 已退款 | `refund_done` | 退款到账 |
| 6 | 退款中 | 1 | 已支付 | `refund_reject` | 退款驳回恢复 |
| 6 | 退款中 | 5 | 已取消 | `cancel` | 退款流程取消订单 |

`machine_code='ORDER_SM'`，`biz_type='order'`。P7-基础任务负责重写种子 SQL（删旧 INSERT 块、写新块），保留注释头格式。

> **注意**：P5 的 SERVICE_SESSION_SM、P4 的 EQUITY_SM、P3 的 PARK_SM 种子**不动**，仅重写 ORDER_SM 这一段。

### 0.4 状态/字典常量（直写枚举，不查字典表）

订单 `order_status`、结算 `bill_status`、`invoice_status`、`pay_status`、`refund_status`、`account_status`、`flow_type`、`recon` 各 status 均为简单整型，**业务层用枚举常量类固化**（参考 `EquityEvent` 模式），不走 StateMachineEngine（结算域状态字段无 SM 种子，直接校验取值范围）。仅订单 `order_status` 走 ORDER_SM 状态机。

### 0.5 跨模块数据访问——只读 View 模式

订单状态变更日志表 `system_order_status_log` 属系统域（共享表，AUTO_INCREMENT）。订单域需向其写日志，但**不依赖系统域模块**（避免耦合）。采用 P3/P5 已确立的 **View 模式**：在订单域内新建 `OrderStatusLogView` 实体（`@TableName("system_order_status_log")`，`IdType.AUTO`，因该表是 AUTO_INCREMENT）+ 对应 ViewMapper，直接读写该表。

> 同理：订单域不依赖结算域；结算域不依赖订单域。订单与结算的资金联动（支付写 payment、退款写 refund）**本期不做跨模块调用**，由订单状态机驱动订单自身状态，支付/退款记录由结算域独立提供创建接口（在结算域内部完成），跨域编排留给 P8+ 网关/事件层。

### 0.6 编码生成

参考 `EquityDepotServiceImpl`：注入 `private final SequenceProvider sequenceProvider;`，调用 `sequenceProvider.next(key)` 取 Redis INCR 序列，按 `前缀 + 日期 + 0 填充序号` 拼装。`CodeGenerator`（实例 Bean）的 `generate(prefix)` 也可用，二选一，**与 equity 一致用 SequenceProvider**（已验证）。

订单编号格式（DDL 注释）：`OD + 年月日(8) + 4位序号` → 如 `OD202608040001`。  
其余编码（flow/bill/invoice/payment/refund/account/recon）前缀建议：
- flow: `FL` / bill: `BL` / invoice: `IV` / payment: `PAY` / refund: `RF` / account: `ACC` / recon: `RC`

### 0.7 金额与 BigDecimal

所有金额字段（unit_price/total_amount/pay_amount/flow_amount 等）用 `BigDecimal`，**禁止用 double/float**。金额校验：`total_amount = unit_price * quantity`（权益/课程），或各项费用加和（旅居 room_fee+care_fee+food_fee+other_fee），`pay_amount = total_amount - discount_amount - coupon_amount`。

### 0.8 通用基础类与返回

- 实体继承 `com.dayan.common.mybatis.entity.BaseEntity`（含 createdAt/updatedAt/creator/updater/deleted/deletedAt + 逻辑删除 + 自动填充）。
- Service 抛 `BusinessException(ErrorCode.XXX, "中文提示")`；Controller 返回 `R<T>` / `R<PageResult<T>>`。
- 分页：`Page<T>` + `PageResult<T>`（参考 equity `page()`）。
- 控制器分端：本期实现 **admin** 端（全局管理）；channel/agent/client/distributor/supplier/open 端仅留 package-info，不实现（与 P4-P6 节奏一致，多端在 P8+ 统一接入）。

### 0.9 编译与提交

- 单模块编译必须用 reactor：`mvn -B -ntp -pl dayan-server/dayan-modules/dayan-module-finance -am compile`（`-am` 带依赖）。最终验证：`mvn -B -ntp clean compile`（全 41 模块）。
- 提交到 main 分支（单人仓库，无 PR）。提交信息：`feat(p7-x): ...`。

---

## 1. 订单域（dayan-module-order）—— P7-B

### 1.1 表与字段要点

| 表 | 关键字段 | 业务语义 |
|----|---------|---------|
| order_equity | order_source(1对公/2个人)、deliver_type(1批量/2逐张/3自动入库)、deliver_count、expire_time | 权益采购订单，支付后触发权益入库（与 P4 equity_depot 联动，本期仅维护订单侧 deliver_count） |
| order_scene | activity_date、participant_count、participant_names(JSON)、schedule_code、equity_code、coupon_code | 场景报名订单，支持权益抵扣 + 优惠券 |
| order_course | course_code、quantity、equity_code、coupon_code | 课程购买订单，支持权益兑换（order_type=3） |
| order_sojourn | park_code、room_type_code、checkin/checkout_date、stay_days、room/care/food/other_fee、deposit_amount | 旅居房间预订，含照护/餐饮附加费 |

4 表共享 `order_status`（8 态 ORDER_SM）、`pay_type`(1微信/2支付宝/3银行转账/4余额/5线下)、`channel_code/agent_code/distributor_code`（快照fullName）。

### 1.2 Service 层（每表一个 Service + Impl）

- `OrderEquityService` / `OrderSceneService` / `OrderCourseService` / `OrderSojournService`
- 每个 Service 含：`page(query)` / `list(query)` / `getDetail(orderCode)` / `create(CreateXxxDTO)` / `payCallback(PayCallbackDTO)` / `applyRefund(RefundApplyDTO)` / `complete(orderCode)` / `cancel(orderCode, reason)`
- **状态流转统一经 ORDER_SM**：`stateMachineEngine.transition(OrderEvent.DOMAIN, from, event)`，`DOMAIN="ORDER_SM"`。
- **核心链路 create**（@Transactional）：生成订单号(OD+日期+序号) → 校验金额(见 0.7) → 计算快照(channelFullName/agentFullName 等) → 置 order_status=0(待支付) + expire_time(默认30分钟，权益订单可配) → insert → 写 status_log(0→0 初始，或 create 事件)。
- **核心链路 payCallback**（@Transactional）：查订单 → from=0 时 `transition(pay)` → 1(已支付) → 写 pay_time/pay_trade_no/pay_type → 写 status_log(0→1)。
- **核心链路 applyRefund**：from∈{1,2,3} 时 `transition(refund_apply)` → 6(退款中) → 写 status_log。
- **核心链路 cancel**：from=0 时 `transition(cancel)` → 5(已取消) + cancel_reason；from=6 时（退款中取消）`transition(cancel)` → 5。
- **核心链路 complete**：from=3 时 `transition(complete)` → 4。
- **delivered/partialDeliver**（权益订单特有）：deliver_type + 出库联动 → `transition(partial_deliver)` 0?1→2 或 `transition(deliver)` 1→3（权益订单履约由 equity 域出库驱动，本期订单侧提供接口供调用，实现内仅做状态机流转 + deliver_count 更新）。

### 1.3 OrderEvent 枚举（enums/OrderEvent.java）

```java
public final class OrderEvent {
    public static final String DOMAIN = "ORDER_SM";
    // 事件
    public static final String PAY = "pay";
    public static final String CANCEL = "cancel";
    public static final String PARTIAL_DELIVER = "partial_deliver";
    public static final String DELIVER = "deliver";
    public static final String COMPLETE = "complete";
    public static final String REFUND_APPLY = "refund_apply";
    public static final String REFUND_DONE = "refund_done";
    public static final String REFUND_REJECT = "refund_reject";
    // 状态码
    public static final int STATUS_PENDING_PAY = 0;
    public static final int STATUS_PAID = 1;
    public static final int STATUS_PARTIAL_DELIVERED = 2;
    public static final int STATUS_DELIVERED = 3;
    public static final int STATUS_COMPLETED = 4;
    public static final int STATUS_CANCELLED = 5;
    public static final int STATUS_REFUNDING = 6;
    public static final int STATUS_REFUNDED = 7;
}
```

### 1.4 OrderStatusLogView（跨域只读写）

`entity/OrderStatusLogView.java`：`@TableName("system_order_status_log")`，`IdType.AUTO`（该表 AUTO_INCREMENT），字段对齐 system_order_status_log。  
`mapper/OrderStatusLogViewMapper.java`：`extends BaseMapper<OrderStatusLogView>`。  
订单 Service 注入此 Mapper，每次状态流转后 insert 一条日志（from/to/reason/operator）。

### 1.5 DTO/VO/Controller

- DTO：每表 `XxxCreateDTO`、`XxxQueryDTO`、`PayCallbackDTO`、`RefundApplyDTO`、`XxxDeliverDTO`（权益）、`XxxCompleteDTO`（按需）。create DTO 用 `@NotNull/@NotBlank` + `@Valid`。
- VO：`XxxOrderVO`（脱敏敏感字段如 payTradeNo 可保留，无密钥类字段）。
- Controller：`admin/OrderEquityAdminController` 等 4 个，`@RequestMapping("/order/equity")` 等，提供 page/list/detail/create/pay-callback/apply-refund/cancel/complete。权益订单额外 deliver/partial-deliver。

---

## 2. 结算域（dayan-module-finance）—— P7-A

### 2.1 表与字段要点

| 表 | 关键字段 | 业务语义 |
|----|---------|---------|
| finance_flow | flow_type(1收入/2支出/3退款/4结算)、biz_type(字符串)、account_type、balance_before/after、is_settled、settle_code | 财务流水（账户余额变动） |
| finance_bill | bill_type(1渠道/2供应商)、target_type、period_start/end、order_count、commission/refund/adjust_amount、final_amount、bill_status(0待审/1通过/2结算中/3已结/4拒)、flow_ids(JSON) | 结算单（周期汇总） |
| finance_invoice | invoice_type、applicant_type、title_type、invoice_title、tax_no、invoice_status(0待审/1已审/2已开/3已寄/4完成/5作废/6红冲) | 发票 |
| finance_account | direction(1应收/2应付)、total/received/remain_amount、due_date、account_status(0待/1部分/2结清/3逾期/4坏账) | 应收应付账目 |
| finance_reconciliation | recon_type、our/their_order_count、diff_count/amount、recon_result(0差异/1一致)、status(0对账中/1完成/2待确认/3已确认) | 对账记录 |
| finance_payment | order_type/order_code、pay_type、pay_amount、trade_no、pay_status(0待/1成功/2失败/3已退/4部分退) | 支付记录（v4.2 从订单域迁入） |
| finance_refund | order_type/order_code、refund_amount、refund_type(1全额/2部分)、refund_channel、refund_status(0待审/1通过/2退款中/3成功/4拒/5失败) | 退款记录（v4.2 从订单域迁入） |

### 2.2 Service 层（每表一个 Service + Impl）

- `FinanceFlowService` / `FinanceBillService` / `FinanceInvoiceService` / `FinanceAccountService` / `FinanceReconciliationService` / `FinancePaymentService` / `FinanceRefundService`
- 每个：`page/list/detail` + 核心写操作：
  - **Flow**：`record(RecordFlowDTO)` —— 生成流水号 FL+序号，记 balance_before/after（本期 account 余额若无强一致账户表则 before/after 用 0 占位 + remark 说明，或查 finance_account 同账户最近一条 after），写 flow_time=now。
  - **Payment**：`create(CreatePaymentDTO)` —— 生成 PAY+序号，写 pay_status=0(待支付)；`markSuccess(paymentCode, tradeNo)` —— 0→1；`markFailed` 0→2。
  - **Refund**：`apply(ApplyRefundDTO)` —— 生成 RF+序号，写 refund_status=0(待审核)+apply_time；`audit(refundCode, pass, remark)` —— 0→1(通过) 或 0→4(拒绝)；`markRefunding` 1→2；`markSuccess` 2→3；`markFailed` 2→5。
  - **Bill**：`generate(GenerateBillDTO)` —— 按周期+target 汇总生成结算单（order_count/total/commission/refund/adjust → final_amount），bill_status=0；`audit(billCode, pass)` 0→1/0→4；`startSettle` 1→2；`finishSettle` 2→3。
  - **Invoice**：`apply(ApplyInvoiceDTO)` —— 生成 IV+序号，invoice_status=0；`audit(pass)` 0→1；`issue` 1→2；`send` 2→3；`finish` 3→4；`void` →5；`redFlush` →6。
  - **Account**：`create(CreateAccountDTO)`；`receive(accountCode, amount)` —— 累加 received_amount、扣 remain_amount、推进 account_status（0→1 部分收/2 结清）。
  - **Reconciliation**：`create`；`confirm` 0→3（经 2 待确认）。
- **状态校验**：所有 status 字段直接 if-else 校验合法前置态（不走 SM），非法抛 BusinessException。

### 2.3 FinanceEvent / 常量枚举

`enums/FinanceEvent.java`：固化 flow_type/bill_status/invoice_status/pay_status/refund_status/account_status/direction 各取值常量（`public static final int`），与 DDL 注释逐字对齐。提供 `PayType`、`BizType`、`AccountType` 等字符串常量。

### 2.4 DTO/VO/Controller

- DTO：各表 Create/Query/audit/mark DTO。
- VO：各表 VO（金额类原样，无脱敏需求）。
- Controller：`admin/FinanceXxxAdminController` 共 7 个，`@RequestMapping("/finance/flow")` 等，提供 page/list/detail + 核心写接口。

---

## 3. 任务拆分

| 任务 | 模块 | 内容 | 模型建议 |
|------|------|------|---------|
| P7-base | order+finance+common-mybatis+seed | ① order_/finance_ 加入租户忽略 ② 11 表 Entity AUTO→ASSIGN_ID（含注释） ③ 重写 ORDER_SM 种子 ④ 自审编译 | 控制者亲自做（基础设施） |
| P7-A | dayan-module-finance | 结算域 7 表：Entity/Mapper(已有)/Service/Impl/DTO/VO/Controller(admin)/enums | 标准模型（多文件集成） |
| P7-B | dayan-module-order | 订单域 4 表 + OrderStatusLogView + OrderEvent：Service/Impl/DTO/VO/Controller(admin) + ORDER_SM 集成 | 标准模型（状态机集成） |
| P7-verify | 全量 | `mvn -B -ntp clean compile` 41 模块 | 控制者亲自做 |

**并行性**：P7-A（finance 目录）与 P7-B（order 目录）文件无重叠，可并行分派。但 P7-A/P7-B 都依赖 P7-base（租户忽略 + ASSIGN_ID + ORDER_SM 种子）。**串行**：base → (A‖B) → verify。

---

## 4. 验收标准（审查者用）

1. 11 表 Entity 全部 `IdType.ASSIGN_ID` + 注释"主键（分片表，雪花ID，MyBatis-Plus 自动分配）"。
2. `order_`、`finance_` 在 DayanTenantHandler.DEFAULT_IGNORE_PREFIXES。
3. ORDER_SM 种子状态码与 DDL order_status 注释一致（0待支付/1已支付/2部分发放/3已发放/4已完成/5已取消/6退款中/7已退款），12 规则齐备，其它 3 个 SM 未动。
4. 订单核心链路（create/payCallback/applyRefund/cancel/complete）均 @Transactional + 经 ORDER_SM transition + 写 status_log。
5. 结算域各状态字段取值与 DDL 注释一致，非法流转抛 BusinessException。
6. 金额一律 BigDecimal，金额计算公式正确。
7. 全量 `mvn clean compile` BUILD SUCCESS。
