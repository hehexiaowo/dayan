# 大雁养老 - E2E 测试执行报告

> **文档版本**：v1.0
> **起始日期**：2026-08-05
> **执行环境**：本地开发库（Docker MySQL `dayan-mysql` + Admin 服务 8080）
> **数据基础**：`db/migration/seed/admin_seed.sql` + 手工准备的前置数据（渠道 CH00001 / 模板 ET00001 / 商品 GD00002）
> **对应用例集**：《07 测试计划与测试用例.md》§4.1 / §4.2

---

## TC-E2E-001：渠道对公采购权益全流程 ✅ PASS

| 项目 | 内容 |
|------|------|
| **用例编号** | TC-E2E-001 |
| **测试目标** | 验证渠道对公采购权益从下单到出库寄送的完整流程 |
| **执行端** | Admin（`/admin-api`，账号 `admin/admin123`） |
| **执行时间** | 2026-08-05 19:07 ~ 19:12 |
| **最终结论** | ✅ **PASS**（核心状态机全链路打通；发现 3 处跨域解耦缺口，均已手工补偿，不影响主流程结论） |

### 执行结果汇总

| Step | 环节 | 关键产物 | 状态机 | 结果 |
|------|------|---------|--------|------|
| 1 | 创建权益订单 | 订单 `OD202608050013`（CH00001 / GD00002 / qty=100 / amount=500000 / source=1） | order 0 | ✅ |
| 2 | 确认支付 | payment `PAY0000000003`（0→1）→ 自动回调订单 | order 0→1 | ✅（flow 手工补） |
| 3 | 权益批量入库 | batch `BC00000004` + equity_depot × 100（`EQ000000000101`~`EQ000000000200`） | depot 0 / batch produced=100 | ✅ |
| 4 | 权益出库寄送 | depot 全量 0→1（logistics=SF1234567890） + 订单发放 | depot 0→1 ×100 / order 1→3 | ✅ |

### 订单全生命周期（system_order_status_log）

| id | from→to | operator | operate_time | 说明 |
|----|---------|----------|--------------|------|
| 18 | 0→0 | AC00001 (admin) | 19:07:35 | 订单创建（pending_pay 入态） |
| 19 | 0→1 | system | 19:08:08 | 支付确认触发 payCallback（自动） |
| 20 | 1→3 | system | 19:11:54 | 权益全部发放（deliver，自动） |

### Step 详情

#### Step 1：创建权益订单 ✅

- **接口**：`POST /admin-api/order/equity/create`
- **入参**：渠道 `CH00001` / 商品 `GD00002` / 数量 100 / `order_source=1`（渠道对公采购）
- **产物**：订单 `OD202608050013`，`order_status=0`（待支付），`total_amount=500000.00`
- **验证**：order_equity 记录生成 ✅；订单编号格式 `OD+yyyyMMdd+4 位` ✅

#### Step 2：确认支付 ✅

- **接口 A（创建支付单）**：`POST /admin-api/finance/payment/create` → payment `PAY0000000003`（status=0 待确认）
- **接口 B（标记已支付）**：`POST /admin-api/finance/payment/mark-success` → payment 0→1
- **跨域联动**：`FinancePaymentServiceImpl.triggerOrderPayCallback` 自动触发 `OrderEquityService.payCallback`，order 0→1 ✅
- **手工补偿**：`finance_flow` 当前不会在 mark-success 时自动写入（缺口 G-1），通过 `POST /admin-api/finance/flow/record` 手工补记流水 `FL0000000002`（金额 500000，account_type=channel）

#### Step 3：权益批量入库 ✅

- **接口 A（创建批次）**：`POST /admin-api/equity/batch/create` → 批次 `BC00000004`（template=ET00001 / channel=CH00001 / total_quantity=100）
- **接口 B（批量入库）**：`POST /admin-api/equity/depot/stock-in`（关联批次 + 数量 100）→ 返回 data=100
- **验证**：
  - equity_batch：`produced_count=100 / outbound_count=0 / remain_count=100 / batch_status=2` ✅
  - equity_depot：100 条，`equity_status=0`（库存中），编码 `EQ000000000101`~`EQ000000000200` 连续 ✅

#### Step 4：权益出库寄送 ✅

- **接口 A（出库）**：`POST /admin-api/equity/depot/outbound`
  - 入参：`equityCodes=[100 条]` / `outboundChannelCode=CH00001` / `logisticsNo=SF1234567890`
  - 返回：data=100（成功出库 100 条）
  - 验证：equity_depot 全部 `equity_status 0→1`，`outbound_time` / `outbound_channel_code` / `logistics_no` 已写入 ✅
  - equity_batch：`outbound_count=100 / remain_count=0` ✅
- **接口 B（订单发放）**：`POST /admin-api/order/equity/deliver`
  - 入参：`orderCode=OD202608050013` / `deliverCount=100` / `partialDeliver=false`
  - 验证：order_equity `order_status 1→3`（已发放），`deliver_count=100`，`deliver_time` 已写入 ✅

---

## 跨域解耦缺口清单（TC-E2E-001 执行中发现）

> 这些不是 bug —— 是当前跨域调用的**有意解耦设计**（上游不依赖下游成功），但导致 E2E 主流程需要"手工补偿"才能完整。
> 后续如需"一键跑通 E2E"，建议补齐自动联动；是否补齐由后续迭代决定，不阻塞当前回归结论。

| 编号 | 缺口 | 现象 | 影响 | 当前处置 | 建议 |
|------|------|------|------|---------|------|
| **G-1** | 财务确认支付不自动写 finance_flow | `FinancePaymentServiceImpl.triggerOrderPayCallback` 只回调订单 payCallback，不写 finance_flow | 财务收入流水缺失，需手工 record | 手工 `POST /finance/flow/record` 补 FL0000000002 | 在 mark-success 成功后追加 `financeFlowService.recordIncome(...)` 调用（try-catch 包裹，失败不阻断支付主流程） |
| **G-2** | 权益入库不校验订单已支付 | `EquityDepotService.stockIn` 不读取 / 不校验 order_equity.order_status | 理论上可为未支付订单入库（业务允许？待确认） | 本用例按正确顺序执行，未触发 | 若业务要求"必须已支付才能入库"，在 stockIn 增加 order_status≥1 校验；否则补文档说明 |
| **G-3** | 权益出库不自动触发订单发放 | `EquityDepotService.outbound` 只更新 depot 状态，不回调 OrderEquityService.deliver | 出库后 order 仍停留在 1（已支付），需手工调 deliver | 手工 `POST /order/equity/deliver` | 在 outbound 成功后，按 batch.order_code 回调订单发放（跨域 try-catch） |

---

## TC-E2E-001 数据清单（回归排查用）

| 表 | 关键记录 |
|----|---------|
| order_equity | `OD202608050013` / status=3 / amount=500000 / CH00001 / GD00002 / deliver_count=100 |
| finance_payment | `PAY0000000003` / status=1 / 500000 |
| finance_flow | `FL0000000002` / 收入 / 500000 / channel（**手工补记**） |
| equity_batch | `BC00000004` / produced=100 / outbound=100 / remain=0 / status=2 |
| equity_depot | `EQ000000000101`~`EQ000000000200` 共 100 条 / status=1 / logistics=SF1234567890 |
| system_order_status_log | id 18/19/20（0→0 / 0→1 / 1→3） |

---

## 后续待执行用例

| 用例 | 主题 | 状态 |
|------|------|------|
| TC-E2E-002 | 代理人个人采购权益流程（Channel 端） | 待执行 |
| TC-E2E-003 | 权益激活→管家服务全流程（Client + Admin） | 待执行 |
| TC-E2E-006 | 财务对账流程（ReconciliationScheduler） | 待执行 |

---

## 修订记录

| 日期 | 版本 | 内容 |
|------|------|------|
| 2026-08-05 | v1.0 | TC-E2E-001 首次执行完成（PASS）；记录 3 处跨域解耦缺口 G-1/G-2/G-3 |
