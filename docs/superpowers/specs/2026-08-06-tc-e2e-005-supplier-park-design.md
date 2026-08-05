# TC-E2E-005 联调设计规格：供应商入驻→机构上线全流程

> 阶段：全局 E2E 拆分第 1 轮（按 E2E 用例为主线推进）
> 对应用例：《07 测试计划与测试用例.md》§4.4 TC-E2E-005
> 执行端：**Admin（`/admin-api`，账号 `admin/admin123`）**——供应商走 Admin 代录，不实现供应商自助登录
> 前置规格依据：本规格为新建，无上游 spec

---

## 0. 背景与定位

### 0.1 全局拆分主线

本项目剩余工作横跨 3 端（Channel/Agent/Client）× 17 业务域的后端业务 controller 缺口。经评估，以 **E2E 用例** 为主线推进，每轮产出可端到端验证的成果。三个阻塞用例的推进顺序：

```
TC-E2E-005（供应商入驻→机构上线）   ← 本轮：纯 Admin 端闭环，零认证依赖
    │
    ↓ （机构上线后，客户端才有机构可浏览）
TC-E2E-003 真实化（Client 端 park 列表）  ← 后续轮次
    │
TC-E2E-004（场景活动全生命周期）    ← 后续轮次：依赖 Agent 端 + scene 域
    │
TC-E2E-007（渠道开放平台 API 对接）  ← 最后：依赖 channel_open_platform 签名体系
```

**TC-E2E-005 排第一的理由**：唯一完全在 Admin 端闭环的 E2E——供应商代录、审核、合同、机构录入、上线全部走 `/admin-api/*`，不依赖任何新认证端、不依赖前端联调（Admin 端 supplier/park 管理页 P8 已交付）。风险最低、见效最快，且产出的"机构数据"是后续 Client 端浏览机构的前置数据。

### 0.2 本轮性质：联调 + 按需补缺，非重新开发

探查确认，TC-E2E-005 五步所需的 Admin 端原子接口**全部已实现**：

| Step | 操作 | 接口 | 实现状态 |
|------|------|------|---------|
| 1 | 供应商入驻申请 | `POST /admin-api/supplier/info`（create） | ✅ |
| 2 | 平台审核 | `POST /admin-api/supplier/info/audit` | ✅ |
| 3 | 合同签署 | `POST /admin-api/supplier/contract`（create） | ✅ |
| 4 | 机构信息录入 | `POST /admin-api/park/info` + 9 类扩展表 CRUD | ✅（15 个 admin controller 齐备）|
| 5 | 机构审核上线 | `POST /admin-api/park/info/transition?event=approve`（PARK_SM 0→1） | ✅ |

因此本轮的本质是：**执行联调 → 识别跨域联动缺口与语义 bug → 按需修复 → 回归验证 → 产出 E2E 报告**。与已完成的 TC-E2E-001~006 及 G-1/3/5/7 修复模式完全一致。

### 0.3 身份模型决策

TC-E2E-005 Step 1 文字描述"供应商联系人可访问 Channel 端注册"，但本轮决策：**供应商走 Admin 代录**——不实现供应商自助登录（`supplier_account`/`StpKit.SUPPLIER` 虽已预留，本轮不启用）。所有 supplier/park 数据由 Admin 账号代为录入。理由：最小化范围、零认证依赖、复用 P8 已交付的 Admin supplier/park 管理页。

---

## 1. 核心发现：supplier.status 语义三方冲突（阻塞性 bug）

### 1.1 冲突描述

`supplier_info` 表的 `status` 字段，在 DDL、测试用例文字、后端实现三处语义完全不一致：

| 来源 | `status` 取值语义 |
|------|------------------|
| **DDL** `db/migration/04_supplier.sql` | `0=待审核, 1=已合作, 2=已暂停, 3=已终止` |
| **TC-E2E-005 文字**（§4.4） | "审核通过 → `status→1`（已合作）" —— 与 DDL 一致 ✅ |
| **`SupplierInfoServiceImpl`** | `STATUS_PENDING_AUDIT=1, AUDIT_PASS=2, AUDIT_REJECT=3` —— **与 DDL 冲突** ❌ |
| **`ParkInfoServiceImpl.validateSupplier`** | `SUPPLIER_STATUS_APPROVED=2` —— 按"已合作=2"判定，与错误实现自洽但与 DDL 相反 ❌ |

### 1.2 根因

`SupplierInfoServiceImpl.audit()` 中：
```java
update.setStatus(auditStatus);   // auditStatus ∈ {2=PASS, 3=REJECT}（AUDIT_* 常量）
update.setAuditStatus(auditStatus);
```
实现把**审核状态值**（audit_status 域：0/1/2）直接写进了 **status 字段**（合作状态域：0/1/2/3），两个字段语义混用。新建供应商时 `entity.setStatus(STATUS_PENDING_AUDIT=1)`，但 DDL 里 status=1 是"已合作"——意味着供应商一创建就"已合作"，语义错误。

### 1.3 修复方案（本轮核心交付）

**以 DDL 为准**（DDL 是数据真相，测试用例与之一致），修正后端实现常量：

| 字段 | 修正后常量（对齐 DDL） |
|------|----------------------|
| `status` | `STATUS_PENDING_AUDIT=0, STATUS_COOPERATING=1, STATUS_SUSPENDED=2, STATUS_TERMINATED=3` |
| `audit_status` | `AUDIT_PENDING=0, AUDIT_PASS=1, AUDIT_REJECT=2` |

`audit()` 方法改为：审核通过时 `status→1（已合作）` + `audit_status→1（审核通过）`；驳回时 `status` 维持 0 + `audit_status→2`。两个字段各归各位。

`ParkInfoServiceImpl.SUPPLIER_STATUS_APPROVED` 由 `2` 改为 `1`（已合作）。

**数据修复**：现有库中由错误实现写入的 supplier 记录（status 值错位）需在联调前手工修正或重新建数据。本轮 E2E 执行前会新建全新供应商，故不涉及历史数据迁移脚本。

---

## 2. 预判联动缺口（待联调实证）

基于 G-1~G-7 的修复模式，TC-E2E-005 链路中以下联动点需在联调中验证。探查已初步确认其状态：

| 编号 | 联动点 | 探查结论 | 本轮处置 |
|------|--------|---------|---------|
| **G-8** | 供应商审核通过后 `status` 自动联动到"已合作" | ❌ **bug**（见 §1，status 被错写为 audit 值） | **修复**：audit() 按 §1.3 重写 |
| **G-9** | 机构 create 校验 supplierCode 已审核通过 | ✅ **已实现**（`validateSupplier` 校验 `status==SUPPLIER_STATUS_APPROVED`） | 仅同步常量值 2→1 |
| **G-10** | 机构 `approve` 上线前校验 supplier 仍为已合作 | ❌ **缺口**（`transition()` 未调 `validateSupplier`） | **修复**：transition 中对 approve 事件追加 supplier 状态校验（防止 supplier 被驳回后机构仍能上线） |
| **G-11** | 机构上线后 `supplier_info.parkCount` 自增 | ❌ **可能缺口**（transition 无更新 supplier 逻辑） | **维持现状**：parkCount 是冗余统计字段，非主流程必需；补文档说明，不阻塞 E2E |
| **G-12** | 机构上线 `is_published` 联动 | ✅ **已实现**（`transition()` 中 `setIsPublished(to==ONLINE?1:0)`） | 无需改动 |

**结论**：本轮实际需修复 2 处（G-8 bug + G-10 缺口），1 处仅常量同步（G-9），2 处已实现无需动（G-12、create 的 validateSupplier 已在），1 处维持现状（G-11）。改动量可控。

---

## 3. 接口契约与数据流（TC-E2E-005 五步）

### 3.1 Step 1：供应商入驻申请

```
POST /admin-api/supplier/info
Content-Type: application/json
权限：supplier:info:create（admin 超管自动持有）

入参（SupplierInfoCreateDTO，必填字段）：
{
  "fullName": "阳光养老服务有限公司",
  "shortName": "阳光养老",
  "supplierType": 1,                    // 1=养老机构
  "unifiedCreditCode": "91110108MA01ABCDEF",
  "contactPerson": "张经理",
  "contactPhone": "13800138000",
  "provinceCode": "110000",
  "cityCode": "110100",
  "districtCode": "110108",
  "address": "北京市海淀区中关村大街1号",
  "licenseImage": "/upload/license/xxx.pdf",
  "qualificationImage": "/upload/qual/xxx.pdf"
}

预期产物：
  - supplier_info 记录，supplier_code = SP+yyyyMMdd+4位（如 SP202608060001）
  - status = 0（待审核）✅ 修复后
  - audit_status = 0（待审核）
验证点：修复前 status 被设为 1（错误），修复后应为 0
```

### 3.2 Step 2：平台审核

```
POST /admin-api/supplier/info/audit
权限：supplier:info:audit

入参（SupplierAuditDTO）：
{
  "supplierCode": "SP202608060001",
  "auditStatus": 1,                     // ⚠️ 修复后：1=审核通过（对齐 DDL audit_status 语义）
  "auditRemark": "资质齐全，审核通过"
}

预期（修复后）：
  - audit_status: 0 → 1（审核通过）
  - status: 0 → 1（已合作）    ← G-8 修复核心：两个字段各归各位
验证点：
  - 修复前：status 被设为 2（AUDIT_PASS 旧值，DDL 语义=已暂停，错误）
  - 修复后：status=1（已合作），audit_status=1（审核通过）
```

> **注意**：`SupplierAuditDTO.auditStatus` 的合法值在修复后由 {2,3} 改为 {1,2}。这是接口契约变更，但当前无前端调用方（P8 supplier 管理页的审核按钮若已对接需同步，联调时确认）。

### 3.3 Step 3：合同签署

```
POST /admin-api/supplier/contract
权限：supplier:contract:create

入参（SupplierContractCreateDTO，必填字段）：
{
  "contractName": "阳光养老-大雁平台机构合作合同",
  "supplierCode": "SP202608060001",
  "contractType": 1,                    // 机构合作
  "effectiveDate": "2026-01-01",
  "expireDate": "2027-12-31",           // 校验 effectiveDate < expireDate
  "settlementCycle": 1,                 // 1=月结
  "commissionRate": 0.10,
  "signPerson": "李法务",
  "status": 1                           // 生效
}

预期：supplier_contract 记录，contract_code 唯一（CT+yyyyMMdd+4位）
验证点：是否校验 supplier.status=1（已合作）才能签合同
  - 探查：SupplierContractServiceImpl.create 是否有 supplier 状态前置校验需联调确认
  - 若无 → 记为辅助缺口，维持现状（不阻塞主流程，Admin 操作员按正确顺序执行）
```

### 3.4 Step 4：机构信息录入

```
主表 POST /admin-api/park/info
权限：park:info:create

入参（ParkInfoCreateDTO，必填字段）：
{
  "supplierCode": "SP202608060001",     // G-9：validateSupplier 校验 status==1
  "fullName": "阳光颐养中心",
  "shortName": "阳光颐养",
  "abilityType": 1,                     // 机构能力类型
  "province": "北京市", "provinceCode": "110000",
  "city": "北京市", "cityCode": "110100",
  "district": "海淀区", "districtCode": "110108",
  "address": "海淀区中关村大街2号",
  "longitude": "116.3265", "latitude": "39.9831",
  "totalBeds": 200, "availableBeds": 50
}

预期：park_info 记录，park_code = PK+yyyyMMdd+4位
  - operate_status = 0（待审核，OPERATE_STATUS_DEFAULT）
  - is_published = 0（待审核）
  - supplier_code 关联正确

扩展表（按 TC-E2E-005 Step 4，每类至少录 1 条样本，证明录入链路通）：
  POST /admin-api/park/room-type   {"parkCode":"PK...","roomTypeName":"单人间",...}
  POST /admin-api/park/room-price  {"parkCode":"PK...","roomTypeId":1,"price":5000,...}
  POST /admin-api/park/care-type   {"parkCode":"PK...","careTypeName":"一级护理",...}
  POST /admin-api/park/care-price  {"parkCode":"PK...","careTypeId":1,"price":2000,...}
  POST /admin-api/park/food-type   {"parkCode":"PK...","foodTypeName":"普通膳食",...}
  POST /admin-api/park/food-price  {"parkCode":"PK...","foodTypeId":1,"price":800,...}
  POST /admin-api/park/media-image {"parkCode":"PK...","imageUrl":"...","title":"大厅"}

各扩展表 controller 均已实现（P3 交付 15 个 admin controller），字段以各 CreateDTO 为准。
```

### 3.5 Step 5：机构审核上线

```
POST /admin-api/park/info/transition?parkCode=PK202608060001&event=approve
权限：park:info:transition

PARK_SM 规则（已 seed）：
  0(待审核) --approve--> 1(已上线)

预期：
  - park_info.operate_status: 0 → 1
  - park_info.is_published: 0 → 1（已实现联动）
  - PARK_SM 状态机引擎校验通过（规则由 system 模块预热到 Redis）

G-10 修复后追加验证：
  - transition(approve) 前校验 supplier.status==1（已合作）
  - 若 supplier 已被驳回（status≠1），approve 应抛 BusinessException
```

---

## 4. 代码改动清单

### 4.1 修改文件（3 个）

| 文件 | 改动 | 关联 |
|------|------|------|
| `dayan-module-supplier/.../service/impl/SupplierInfoServiceImpl.java` | 重写 status/audit 常量；`create()` 设 `status=0`；`audit()` 两个字段分别赋正确值 | §1.3, G-8 |
| `dayan-module-park/.../service/impl/ParkInfoServiceImpl.java` | `SUPPLIER_STATUS_APPROVED` 常量 2→1；`transition()` 对 approve 事件追加 `validateSupplier` 校验 | G-9 常量同步, G-10 |

### 4.2 新增文件（0 个）

- `SupplierInfoView` + `SupplierInfoViewMapper` 已存在（P3 裁定产物），本轮复用，不新增。
- 无新 DTO/VO/Controller——所有接口已实现。

### 4.3 不改动的文件

- `SupplierInfoAdminController` / `SupplierContractAdminController` / `ParkInfoAdminController` 等 controller 层不动（接口契约不变，仅 SupplierAuditDTO.auditStatus 合法值语义变化，见 §3.2 注意事项）。
- DDL 不动（DDL 是正确真相，改实现去对齐它）。
- 前端不动（P8 supplier/park 管理页已交付；若审核弹窗的 auditStatus 传值需调整，在联调阶段确认并记入 E2E 报告，不纳入本轮代码范围）。

---

## 5. 验收标准

1. **bug 修复验证**：新建供应商，status 正确走 `0(待审核)→审核→1(已合作)`，audit_status 独立走 `0→1`，两字段不再混用。
2. **G-10 验证**：对 status≠1 的 supplier 的机构触发 `approve`，应被拒绝并抛 BusinessException。
3. **TC-E2E-005 五步全跑通**：每步产物（编码、状态值）记录到 E2E 报告。
4. **回归不破坏**：`mvn -B -ntp install -DskipTests` 全模块 BUILD SUCCESS；既有 TC-E2E-001/002/006 不受影响（它们不依赖 supplier/park）。
5. **E2E 报告**：追加 TC-E2E-005 章节 + 修订记录 v1.5 到 `docs/test-reports/E2E测试执行报告.md`，含数据清单（supplier_code / contract_code / park_code）供回归排查。

---

## 6. 维持现状的缺口（本轮不修，记档）

| 缺口 | 维持理由 |
|------|---------|
| **G-11** 机构上线后 supplier.parkCount 不自增 | parkCount 是冗余统计展示字段，非主流程必需；真实机构数可由 `SELECT COUNT(*) FROM park_info WHERE supplier_code=?` 实时得出。补文档说明即可 |
| **G-13?** 合同 create 是否校验 supplier 已合作 | 探查未确认；若联调发现无校验，记为辅助缺口维持现状（Admin 操作员按正确顺序执行不阻塞主流程） |
| **供应商自助登录** | 本轮决策走 Admin 代录，`supplier_account`/`StpKit.SUPPLIER` 预留能力不启用，留后续轮次 |

---

## 7. 风险与回滚

- **风险**：`SupplierAuditDTO.auditStatus` 合法值由 {2,3} 改为 {1,2} 是接口语义变更。若 P8 supplier 管理页的审核弹窗已硬编码传 `auditStatus=2` 表示通过，修复后该传值会变成"驳回"。
- **缓解**：联调阶段优先用 curl 直接调接口验证后端逻辑；前端传值问题若存在，记入 E2E 报告并单独修复（属前端范畴，不阻塞本轮后端验收）。
- **回滚**：改动集中在 2 个 Service 文件的常量与方法，git revert 即可完全回滚。
