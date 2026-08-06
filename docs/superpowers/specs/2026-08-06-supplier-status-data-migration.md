# 生产数据迁移评估：supplier_info.status / audit_status 历史错值修正

> 关联：TC-E2E-005 后续跟进项 #3（progress.md）。
> 背景：G-8 修复（commit 6a9a42d）前，`SupplierInfoServiceImpl` 的 `status` 与 `audit_status` 语义混乱，生产库若存在该期间产生的数据，发布前需一次性 SQL 修正。

## 1. 旧实现的错误语义（修复前）

修复前 `SupplierInfoServiceImpl` 常量与写入逻辑（`git show 6a9a42d^`）：

```java
private static final int STATUS_PENDING_AUDIT = 1;   // 旧：待审核=1
private static final int AUDIT_PASS = 2;             // 旧：通过=2
private static final int AUDIT_REJECT = 3;           // 旧：驳回=3

// create():
entity.setStatus(STATUS_PENDING_AUDIT);              // status=1，audit_status 未写（DDL DEFAULT 0）

// audit(auditStatus):
update.setStatus(auditStatus);                       // ← bug：把 auditStatus 值塞进 status
update.setAuditStatus(auditStatus);                  // audit_status = 传入值（2 或 3）
```

核心错误：**`status` 与 `audit_status` 同值写入**，且取值域与 DDL 注释（`status: 0=待审核,1=已合作,2=已暂停,3=已终止` / `audit_status: 0=待审核,1=通过,2=驳回`）完全不符。

## 2. 旧实现可能产生的数据组合

| 业务状态 | 旧 status | 旧 audit_status | 新 status（DDL） | 新 audit_status（DDL） |
|----------|----------|-----------------|------------------|----------------------|
| 新建未审 | 1 | 0 | **0**（待审核） | **0**（待审核） |
| 审核通过 | 2 | 2 | **1**（已合作） | **1**（通过） |
| 审核驳回 | 3 | 3 | **0**（待审核） | **2**（驳回） |

> 注：旧实现 `audit()` 要求 `status==1` 才能流转，故不会出现 status=1 + audit_status∈{2,3} 之外的组合。理论上不存在旧 status=0 的记录（create 强制 1）。

## 3. 修正 SQL（幂等，按 status 旧值分支）

```sql
-- ============================================================
-- supplier_info.status / audit_status 历史错值修正（G-8 发布前一次性迁移）
-- 适用：曾在 G-8 修复（6a9a42d）前运行过旧 SupplierInfoServiceImpl 的环境
-- 幂等性：以旧 status 取值（1/2/3）为分支条件，修正后 status∈{0,1}，
--         再次执行不会匹配旧分支，故幂等。
-- 前置：建议先 SELECT 备份核对，见第 4 节。
-- ============================================================

START TRANSACTION;

-- 旧 status=1（新建未审）→ 新 status=0, audit_status=0
UPDATE supplier_info
SET status = 0, audit_status = 0
WHERE status = 1 AND audit_status = 0;

-- 旧 status=2, audit_status=2（审核通过）→ 新 status=1, audit_status=1
UPDATE supplier_info
SET status = 1, audit_status = 1
WHERE status = 2 AND audit_status = 2;

-- 旧 status=3, audit_status=3（审核驳回）→ 新 status=0, audit_status=2
UPDATE supplier_info
SET status = 0, audit_status = 2
WHERE status = 3 AND audit_status = 3;

COMMIT;
```

## 4. 执行前核对（只读 SELECT，不修改数据）

```sql
-- 4.1 备份待修正记录（导出核对）
SELECT supplier_code, full_name, status AS old_status, audit_status AS old_audit,
       CASE
         WHEN status = 1 AND audit_status = 0 THEN '→ status=0, audit_status=0（待审核）'
         WHEN status = 2 AND audit_status = 2 THEN '→ status=1, audit_status=1（已合作）'
         WHEN status = 3 AND audit_status = 3 THEN '→ status=0, audit_status=2（驳回）'
         ELSE '⚠️ 未预期组合，人工核查'
       END AS migration_plan
FROM supplier_info
WHERE status IN (1, 2, 3)
ORDER BY supplier_code;

-- 4.2 检查是否有未预期组合（应为 0 行）
SELECT supplier_code, status, audit_status
FROM supplier_info
WHERE NOT (status = 1 AND audit_status = 0)
  AND NOT (status = 2 AND audit_status = 2)
  AND NOT (status = 3 AND audit_status = 3)
  AND status IS NOT NULL;
-- 若此查询有结果，说明存在旧实现未覆盖的组合（如手工改库），需人工裁定，不要盲目跑迁移。
```

## 5. 适用范围判断

| 环境 | 是否需迁移 | 说明 |
|------|-----------|------|
| dev（本地） | ❌ 不需要 | E2E 执行前已按新 DDL 重建（`99_seed.sh` 或手工），无历史错值。 |
| 测试/staging | ⚠️ 视情况 | 若用旧代码跑过供应商审核流程且未重建库，需要。重建过则不需要。 |
| **生产** | ✅ **需要** | 若 G-8 修复前生产已有供应商数据，**发布前必须执行**，否则前端列表按新枚举展示会全部错位（旧 status=2 的"已合作"供应商会被前端显示成"已暂停"）。 |

## 6. 风险与回滚

- **风险**：SQL 以 `status`+`audit_status` 双条件匹配，若存在手工改过库的记录（status/audit_status 组合不在预期），第 4.2 节查询会暴露；此时应逐条人工裁定，不要套用批量 UPDATE。
- **回滚**：迁移在事务内（START TRANSACTION / COMMIT），核对有误可 ROLLBACK；已 COMMIT 的可用第 4.1 节导出的 old_status/old_audit 反推回写。
- **关联表**：`park_info.supplier_code` 不存储 supplier.status，仅 transition(approve) 时实时校验，故 supplier.status 修正不影响已上线机构；`supplier_contract.status` 独立枚举，与本迁移无关。
