# TC-E2E-005 供应商入驻→机构上线 实现计划

> **面向 AI 代理的工作者：** 必需子技能：使用 superpowers:subagent-driven-development（推荐）或 superpowers:executing-plans 逐任务实现此计划。步骤使用复选框（`- [ ]`）语法来跟踪进度。

**目标：** 修复 `supplier_info.status` 语义三方冲突的阻塞性 bug（G-8）、补齐机构上线前的供应商状态校验（G-10），并端到端跑通 TC-E2E-005「供应商入驻→机构上线」五步全流程，产出 E2E 报告。

**架构：** 后端 2 个 Service 文件的常量与方法修正（无新增类，无新增接口），对齐 DDL `db/migration/04_supplier.sql` 中 `status`（0=待审核/1=已合作/2=已暂停/3=已终止）与 `audit_status`（0=待审核/1=审核通过/2=审核驳回）两字段语义。跨模块供应商校验复用 park 模块已有的 `SupplierInfoView` 只读映射（P3 裁定产物），不引入模块依赖。

**技术栈：** Spring Boot 3.2 + MyBatis-Plus 3.5 + Sa-Token（后端）

**设计文档：** `docs/superpowers/specs/2026-08-06-tc-e2e-005-supplier-park-design.md`

**验证策略说明：** 本项目无单元测试基础设施（后端无 `spring-boot-starter-test`，既有模块无测试类）。遵循项目既有规范，采用「编译通过 + 端到端 curl 验证」双重保障，与既有模块及 TC-E2E-001~006 执行模式一致。每个写操作任务都包含编译验证步骤。

---

## 文件结构

### 修改（4 个文件）

| 文件 | 职责 |
|------|------|
| `dayan-server/dayan-modules/dayan-module-supplier/src/main/java/com/dayan/supplier/service/impl/SupplierInfoServiceImpl.java` | 重写 status/audit 常量对齐 DDL；`create()` 设 status=0；`audit()` 两字段分别赋值（G-8 核心） |
| `dayan-server/dayan-modules/dayan-module-park/src/main/java/com/dayan/park/service/impl/ParkInfoServiceImpl.java` | `SUPPLIER_STATUS_APPROVED` 常量 2→1；`transition()` 对 approve 事件追加 `validateSupplier` 校验（G-10） |
| `dayan-server/dayan-modules/dayan-module-park/src/main/java/com/dayan/park/entity/SupplierInfoView.java` | 同步字段注释（status 语义更正），无逻辑改动 |
| `docs/test-reports/E2E测试执行报告.md` | 追加 TC-E2E-005 章节 + 修订记录 v1.5 |

### 创建（0 个）

无新增 Java 类。`SupplierInfoView` + `SupplierInfoViewMapper` 已存在（P3 交付），本轮复用。

---

## 任务 1：修复 SupplierInfoServiceImpl 的 status 语义 bug（G-8）

**文件：**
- 修改：`dayan-server/dayan-modules/dayan-module-supplier/src/main/java/com/dayan/supplier/service/impl/SupplierInfoServiceImpl.java`

**背景：** 当前实现把审核状态值（audit_status 域：0/1/2）与合作状态值（status 域：0/1/2/3）混用——`create()` 设 `status=1`（DDL 语义=已合作，错误，应为 0=待审核），`audit()` 把 `auditStatus`（2=通过）直接写进 `status`（DDL 语义=已暂停，错误）。DDL `04_supplier.sql` 是数据真相，需让实现对齐 DDL。

- [ ] **步骤 1：修改类常量定义**

在 `SupplierInfoServiceImpl.java` 中，找到第 40-44 行的常量定义：

```java
    /** 供应商编码前缀 */
    private static final String CODE_PREFIX = "SP";
    /** 初始状态：待审核 */
    private static final int STATUS_PENDING_AUDIT = 1;
    /** 审核状态：通过 */
    private static final int AUDIT_PASS = 2;
    /** 审核状态：驳回 */
    private static final int AUDIT_REJECT = 3;
```

替换为（对齐 DDL：status 0/1/2/3 合作状态，audit_status 0/1/2 审核状态）：

```java
    /** 供应商编码前缀 */
    private static final String CODE_PREFIX = "SP";
    // ====== status 字段（合作状态，DDL：0=待审核/1=已合作/2=已暂停/3=已终止）======
    /** status：待审核 */
    private static final int STATUS_PENDING_AUDIT = 0;
    /** status：已合作 */
    private static final int STATUS_COOPERATING = 1;
    // ====== audit_status 字段（审核状态，DDL：0=待审核/1=审核通过/2=审核驳回）======
    /** audit_status：审核通过 */
    private static final int AUDIT_PASS = 1;
    /** audit_status：审核驳回 */
    private static final int AUDIT_REJECT = 2;
```

**变更说明：** `STATUS_PENDING_AUDIT` 1→0；新增 `STATUS_COOPERATING=1`；`AUDIT_PASS` 2→1；`AUDIT_REJECT` 3→2。两个状态域彻底分离。

- [ ] **步骤 2：修改 create() 的 status 初值**

在 `create()` 方法中，找到（约第 115 行）：

```java
        entity.setStatus(STATUS_PENDING_AUDIT);
        entity.setAuditStatus(0);
```

确认无需修改——`STATUS_PENDING_AUDIT` 现在等于 0，语义已正确（新建供应商为"待审核"）。但审计：`setAuditStatus(0)` 也保持 0（待审核），两字段一致。此处代码字面不变，仅靠常量值变更生效。

- [ ] **步骤 3：重写 audit() 方法**

在 `audit()` 方法中，找到完整方法体：

```java
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void audit(SupplierAuditDTO dto) {
        SupplierInfo existing = requireSupplier(dto.getSupplierCode());
        Integer auditStatus = dto.getAuditStatus();
        if (auditStatus == null || (auditStatus != AUDIT_PASS && auditStatus != AUDIT_REJECT)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "审核状态非法（仅支持 2=通过 / 3=驳回）");
        }
        // 仅当前 status=1 待审核才能审核
        if (existing.getStatus() == null || existing.getStatus() != STATUS_PENDING_AUDIT) {
            throw new BusinessException(ErrorCode.BUSINESS,
                    "供应商当前状态不可审核（需为待审核状态）: supplierCode=" + dto.getSupplierCode());
        }

        SupplierInfo update = new SupplierInfo();
        update.setId(existing.getId());
        update.setStatus(auditStatus);
        update.setAuditStatus(auditStatus);
        update.setAuditRemark(dto.getAuditRemark());
        supplierInfoMapper.updateById(update);
        log.info("审核供应商完成: supplierCode={}, auditStatus={}", dto.getSupplierCode(), auditStatus);
    }
```

替换为（两字段各归各位）：

```java
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void audit(SupplierAuditDTO dto) {
        SupplierInfo existing = requireSupplier(dto.getSupplierCode());
        Integer auditStatus = dto.getAuditStatus();
        if (auditStatus == null || (auditStatus != AUDIT_PASS && auditStatus != AUDIT_REJECT)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "审核状态非法（仅支持 1=审核通过 / 2=审核驳回）");
        }
        // 仅当前 status=0 待审核才能审核
        if (existing.getStatus() == null || existing.getStatus() != STATUS_PENDING_AUDIT) {
            throw new BusinessException(ErrorCode.BUSINESS,
                    "供应商当前状态不可审核（需为待审核状态）: supplierCode=" + dto.getSupplierCode());
        }

        SupplierInfo update = new SupplierInfo();
        update.setId(existing.getId());
        // status 与 audit_status 两字段语义分离，各归各位：
        // - 审核通过 → status 升级为已合作(1)，audit_status 记审核通过(1)
        // - 审核驳回 → status 维持待审核(0)，audit_status 记审核驳回(2)
        if (auditStatus == AUDIT_PASS) {
            update.setStatus(STATUS_COOPERATING);
        } else {
            update.setStatus(STATUS_PENDING_AUDIT);
        }
        update.setAuditStatus(auditStatus);
        update.setAuditRemark(dto.getAuditRemark());
        supplierInfoMapper.updateById(update);
        log.info("审核供应商完成: supplierCode={}, auditStatus={}, status={}",
                dto.getSupplierCode(), auditStatus, update.getStatus());
    }
```

**变更说明：**
1. 错误提示文案 `2=通过 / 3=驳回` → `1=审核通过 / 2=审核驳回`。
2. 注释 `status=1 待审核` → `status=0 待审核`。
3. **核心修复**：`update.setStatus(auditStatus)`（混用）改为按审核结果分支赋值——通过则 `STATUS_COOPERATING(1)`，驳回则维持 `STATUS_PENDING_AUDIT(0)`。`audit_status` 单独赋审核值。
4. 日志补充 status 输出便于排查。

- [ ] **步骤 4：修改类顶部 Javadoc 注释**

找到类顶部的 Javadoc（第 17-23 行附近）：

```java
/**
 * 供应商信息服务实现。
 *
 * <p>审核流程：status 1=待审核 / 2=已通过 / 3=已驳回。新建默认 status=1，
 * 审核（{@link #audit}）仅允许当前 status=1 时流转。
 *
 * <p>信用代码唯一校验：{@code unifiedCreditCode} 在同 {@code supplierType} 内唯一。
 */
```

替换为：

```java
/**
 * 供应商信息服务实现。
 *
 * <p>审核流程对齐 DDL（db/migration/04_supplier.sql）两字段语义：
 * <ul>
 *   <li>{@code status}（合作状态）：0=待审核 / 1=已合作 / 2=已暂停 / 3=已终止。新建默认 0。</li>
 *   <li>{@code audit_status}（审核状态）：0=待审核 / 1=审核通过 / 2=审核驳回。</li>
 * </ul>
 * 审核（{@link #audit}）仅允许当前 status=0（待审核）时流转：通过则 status→1（已合作），
 * 驳回则 status 维持 0。
 *
 * <p>信用代码唯一校验：{@code unifiedCreditCode} 在同 {@code supplierType} 内唯一。
 */
```

- [ ] **步骤 5：编译验证**

运行：
```bash
cd F:/code/dayan/dayan-server && mvn -pl dayan-modules/dayan-module-supplier -am compile -q
```
预期：BUILD SUCCESS，无编译错误。

- [ ] **步骤 6：Commit**

```bash
cd F:/code/dayan && git add dayan-server/dayan-modules/dayan-module-supplier/src/main/java/com/dayan/supplier/service/impl/SupplierInfoServiceImpl.java
git commit -m "fix(supplier): 修复 status/audit_status 语义混用 bug（G-8）

DDL 定义 status(0=待审核/1=已合作/2=已暂停/3=已终止) 与 audit_status(0/1/2)
两字段，原实现把审核值误写进 status 字段，导致新建即'已合作'、审核后'已暂停'。
改为两字段各归各位：审核通过 status→1，驳回维持 0。"
```

---

## 任务 2：补齐 ParkInfoServiceImpl 的机构上线前置校验（G-9 常量同步 + G-10）

**文件：**
- 修改：`dayan-server/dayan-modules/dayan-module-park/src/main/java/com/dayan/park/service/impl/ParkInfoServiceImpl.java`

**背景：** `SUPPLIER_STATUS_APPROVED` 常量原值为 2（旧错误语义），任务 1 修正后供应商"已合作"对应 status=1，需同步常量。同时 `transition()` 的 `approve` 事件（机构上线）当前不校验供应商是否仍为已合作状态，存在"供应商被驳回后机构仍能上线"的漏洞（G-10）。

- [ ] **步骤 1：修改 SUPPLIER_STATUS_APPROVED 常量值**

在 `ParkInfoServiceImpl.java` 中，找到第 46-47 行：

```java
    /** 供应商已通过审核的状态值 */
    private static final int SUPPLIER_STATUS_APPROVED = 2;
```

替换为：

```java
    /** 供应商已合作状态值（对齐 DDL：status=1=已合作；任务 1 修正后语义） */
    private static final int SUPPLIER_STATUS_APPROVED = 1;
```

**变更说明：** 常量值 2→1。`validateSupplier()` 方法逻辑不变（仍校验 `status == SUPPLIER_STATUS_APPROVED`），只是判定基准跟随修正。

- [ ] **步骤 2：在 transition() 中为 approve 事件追加供应商校验**

找到 `transition()` 方法（第 241-259 行）：

```java
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer transition(String parkCode, String event) {
        ParkInfo existing = requirePark(parkCode);
        Integer from = existing.getOperateStatus();
        int currentFrom = from == null ? OPERATE_STATUS_DEFAULT : from;

        // 调用状态机引擎校验并取得目标状态（PARK_SM 规则已由 system 模块预热到 Redis）
        int to = stateMachineEngine.transition(SM_DOMAIN, currentFrom, event);

        ParkInfo update = new ParkInfo();
        update.setId(existing.getId());
        update.setOperateStatus(to);
        // 联动 is_published：仅已上线(1)对外可见
        update.setIsPublished(to == OPERATE_STATUS_ONLINE ? 1 : 0);
        parkInfoMapper.updateById(update);
        log.info("机构状态流转: parkCode={}, {} --{}--> {}", parkCode, currentFrom, event, to);
        return to;
    }
```

在 `requirePark(parkCode)` 之后、`stateMachineEngine.transition` 之前，插入供应商校验（仅 approve 事件需要，因为只有上线会让机构对外可见）。修改后的方法体：

```java
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer transition(String parkCode, String event) {
        ParkInfo existing = requirePark(parkCode);
        Integer from = existing.getOperateStatus();
        int currentFrom = from == null ? OPERATE_STATUS_DEFAULT : from;

        // G-10：机构上线（approve）前校验供应商仍为已合作状态，防止供应商被驳回后机构仍能上线。
        // 仅对 approve 事件校验（其他事件如 offline/online/suspend/resume 不改变机构对外合法性前提）。
        if ("approve".equals(event)) {
            validateSupplier(existing.getSupplierCode());
        }

        // 调用状态机引擎校验并取得目标状态（PARK_SM 规则已由 system 模块预热到 Redis）
        int to = stateMachineEngine.transition(SM_DOMAIN, currentFrom, event);

        ParkInfo update = new ParkInfo();
        update.setId(existing.getId());
        update.setOperateStatus(to);
        // 联动 is_published：仅已上线(1)对外可见
        update.setIsPublished(to == OPERATE_STATUS_ONLINE ? 1 : 0);
        parkInfoMapper.updateById(update);
        log.info("机构状态流转: parkCode={}, {} --{}--> {}", parkCode, currentFrom, event, to);
        return to;
    }
```

**变更说明：** 仅在 `requirePark` 之后插入 4 行 `if ("approve".equals(event))` 校验块。复用已有的 `validateSupplier(supplierCode)` 私有方法——它已校验"供应商存在 + status==1（已合作）"，失败抛 BusinessException。其余逻辑不变。

**设计要点：**
- 为何只在 `approve` 校验：approve 是机构从"待审核"转为"已上线"对外可见的关键节点，此时必须确保供应商资质仍有效。而 offline/suspend 等是已上线机构的运营态变更，不涉及对外可见性的首次授予。
- 为何用 `validateSupplier` 而非内联查询：DRY，复用 create() 已用的同一校验路径，保证"录入时"和"上线时"判定基准一致。

- [ ] **步骤 3：编译验证全模块**

运行：
```bash
cd F:/code/dayan/dayan-server && mvn -pl dayan-modules/dayan-module-park -am compile -q
```
预期：BUILD SUCCESS。

- [ ] **步骤 4：Commit**

```bash
cd F:/code/dayan && git add dayan-server/dayan-modules/dayan-module-park/src/main/java/com/dayan/park/service/impl/ParkInfoServiceImpl.java
git commit -m "fix(park): 机构上线补齐供应商状态校验（G-10）+ 同步常量值（G-9）

- SUPPLIER_STATUS_APPROVED 2→1 对齐任务 1 修正后的 status 语义
- transition(approve) 追加 validateSupplier 校验，防止供应商被驳回后机构仍能上线"
```

---

## 任务 3：同步 SupplierInfoView 注释

**文件：**
- 修改：`dayan-server/dayan-modules/dayan-module-park/src/main/java/com/dayan/park/entity/SupplierInfoView.java`

**背景：** 该类的类注释和字段注释仍写着旧的错误语义（"status=2 已通过"），需同步以免误导后续维护者。无逻辑改动。

- [ ] **步骤 1：修改类注释**

找到类 Javadoc（第 5-10 行附近）：

```java
/**
 * supplier_info 表只读视图 POJO（跨模块轻量引用）。
 *
 * <p>park 模块不依赖 dayan-module-supplier（避免循环依赖），但需校验
 * {@code park_info.supplier_code} 关联的供应商存在且 status=2（已通过）。
 * supplier_info 为平台共享表（{@code DayanTenantHandler} 忽略 supplier_ 前缀），
 * 多模块映射同一张物理表不冲突，故在此建立最小只读映射，仅含校验所需字段。
 */
```

替换为：

```java
/**
 * supplier_info 表只读视图 POJO（跨模块轻量引用）。
 *
 * <p>park 模块不依赖 dayan-module-supplier（避免循环依赖），但需校验
 * {@code park_info.supplier_code} 关联的供应商存在且 status=1（已合作）。
 * supplier_info 为平台共享表（{@code DayanTenantHandler} 忽略 supplier_ 前缀），
 * 多模块映射同一张物理表不冲突，故在此建立最小只读映射，仅含校验所需字段。
 *
 * <p>status 语义对齐 DDL（db/migration/04_supplier.sql）：0=待审核/1=已合作/2=已暂停/3=已终止。
 */
```

- [ ] **步骤 2：修改字段注释**

找到 status 字段（文件末尾附近）：

```java
    /** 审核状态：1=待审核 / 2=已通过 / 3=已驳回 */
    private Integer status;
```

替换为：

```java
    /** 合作状态（对齐 DDL）：0=待审核 / 1=已合作 / 2=已暂停 / 3=已终止 */
    private Integer status;
```

- [ ] **步骤 3：编译验证**

运行：
```bash
cd F:/code/dayan/dayan-server && mvn -pl dayan-modules/dayan-module-park -am compile -q
```
预期：BUILD SUCCESS（纯注释改动，仅确认无意外触碰代码）。

- [ ] **步骤 4：Commit**

```bash
cd F:/code/dayan && git add dayan-server/dayan-modules/dayan-module-park/src/main/java/com/dayan/park/entity/SupplierInfoView.java
git commit -m "docs(park): 同步 SupplierInfoView 注释为正确的 status 语义"
```

---

## 任务 4：全量编译回归

**依赖：** 任务 1、2、3 全部完成。

- [ ] **步骤 1：全模块编译**

运行：
```bash
cd F:/code/dayan/dayan-server && mvn -B -ntp -DskipTests install
```
预期：全部模块 BUILD SUCCESS。

若报错，常见原因：
- supplier 模块其他类引用了旧常量名（理论上不应有，常量是 private）→ 检查是否遗漏。
- park 模块其他类引用 `SUPPLIER_STATUS_APPROVED`（private，不应有外部引用）→ 检查。

- [ ] **步骤 2：确认工作区状态**

运行：
```bash
cd F:/code/dayan && git status
```
预期：clean（任务 1-3 已各自 commit）。若任务 5 的 E2E 报告尚未写，工作区应只剩 gui-test-screenshots/（会话开始时已存在的未跟踪目录）。

---

## 任务 5：端到端执行 TC-E2E-005 并产出报告

**依赖：** 任务 1-4 全部完成，后端服务可启动（需本地 Docker MySQL `dayan-mysql` + Admin 服务 8080）。

> **执行方式说明：** 本任务为手工端到端联调，使用 curl 直接调 `/admin-api` 接口。Admin 服务需先重启加载任务 1-3 的代码变更。前置数据：`db/migration/seed/admin_seed.sql` 已加载，admin/admin123 账号可用。

- [ ] **步骤 1：启动后端并登录获取 Token**

启动 Admin 服务（端口 8080），确认启动日志含 PARK_SM 规则加载、无报错。

登录获取 Token：
```bash
curl -s -X POST http://localhost:8080/admin-api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'
```
预期：返回 JSON 含 `data.token`。提取 Token 存入变量供后续步骤用：
```bash
TOKEN=<上一步返回的 token 值>
```

- [ ] **步骤 2：Step 1 — 供应商入驻申请**

```bash
curl -s -X POST http://localhost:8080/admin-api/supplier/info \
  -H "Content-Type: application/json" \
  -H "Admin-Token: $TOKEN" \
  -d '{
    "fullName": "阳光养老服务有限公司",
    "shortName": "阳光养老",
    "supplierType": 1,
    "unifiedCreditCode": "91110108MA01E2E005",
    "contactPerson": "张经理",
    "contactPhone": "13800138005",
    "provinceCode": "110000",
    "cityCode": "110100",
    "districtCode": "110108",
    "address": "北京市海淀区中关村大街1号"
  }'
```
预期：
- 返回 `data` 为新建 supplier_code（格式 `SP+yyyyMMdd+4位`），记为 `SP_NEW`。
- **验证 G-8 修复**：查 DB 确认 `status=0`（待审核）、`audit_status=0`（待审核）：
```bash
docker exec dayan-mysql mysql -uroot -proot dayan -e \
  "SELECT supplier_code, status, audit_status FROM supplier_info WHERE supplier_code='SP_NEW'"
```
预期输出：status=0, audit_status=0（修复前会是 status=1，错误）。

- [ ] **步骤 3：Step 2 — 平台审核（G-8 核心验证）**

```bash
curl -s -X POST http://localhost:8080/admin-api/supplier/info/audit \
  -H "Content-Type: application/json" \
  -H "Admin-Token: $TOKEN" \
  -d '{"supplierCode":"SP_NEW","auditStatus":1,"auditRemark":"资质齐全，审核通过"}'
```
预期：返回 `code:0`（成功）。

**验证 G-8 修复核心**——两字段各归各位：
```bash
docker exec dayan-mysql mysql -uroot -proot dayan -e \
  "SELECT supplier_code, status, audit_status, audit_remark FROM supplier_info WHERE supplier_code='SP_NEW'"
```
预期输出：
- `status=1`（已合作）← 修复核心
- `audit_status=1`（审核通过）
- 修复前会是 status=2（AUDIT_PASS 旧值，DDL 语义=已暂停）、audit_status=2，两字段同值且语义错误。

- [ ] **步骤 4：Step 3 — 合同签署**

```bash
curl -s -X POST http://localhost:8080/admin-api/supplier/contract \
  -H "Content-Type: application/json" \
  -H "Admin-Token: $TOKEN" \
  -d '{
    "contractName": "阳光养老-大雁平台机构合作合同",
    "supplierCode": "SP_NEW",
    "contractType": 1,
    "effectiveDate": "2026-01-01",
    "expireDate": "2027-12-31",
    "settlementCycle": 1,
    "commissionRate": 0.10,
    "signPerson": "李法务",
    "status": 1
  }'
```
预期：返回 `data` 为新建 contract_code（格式 `CT+yyyyMMdd+4位`），记为 `CT_NEW`。

观察点 G-13：若 supplier.status≠1 时合同仍能创建成功 → 记为辅助缺口维持现状（不阻塞，Admin 按正确顺序操作不触发）。

- [ ] **步骤 5：Step 4 — 机构信息录入（主表）**

```bash
curl -s -X POST http://localhost:8080/admin-api/park/info \
  -H "Content-Type: application/json" \
  -H "Admin-Token: $TOKEN" \
  -d '{
    "supplierCode": "SP_NEW",
    "fullName": "阳光颐养中心",
    "shortName": "阳光颐养",
    "abilityType": 1,
    "province": "北京市", "provinceCode": "110000",
    "city": "北京市", "cityCode": "110100",
    "district": "海淀区", "districtCode": "110108",
    "address": "海淀区中关村大街2号",
    "longitude": "116.3265", "latitude": "39.9831",
    "totalBeds": 200, "availableBeds": 50
  }'
```
预期：返回 `data` 为新建 park_code（格式 `PK+yyyyMMdd+4位`），记为 `PK_NEW`。

**验证 G-9**（已实现，回归确认）：若返回成功，说明 validateSupplier 通过（supplier.status=1）。查 DB 确认：
```bash
docker exec dayan-mysql mysql -uroot -proot dayan -e \
  "SELECT park_code, supplier_code, operate_status, is_published FROM park_info WHERE park_code='PK_NEW'"
```
预期：operate_status=0（待审核）、is_published=0。

- [ ] **步骤 6：Step 4 — 机构扩展表录入（每类 1 条样本）**

依次执行以下 7 个请求（字段名以各 CreateDTO 为准，下面是最小必填集；若某接口因必填字段不足报 PARAM_ERROR，按报错补字段后重试，记入报告）：

房型 + 房型价格：
```bash
curl -s -X POST http://localhost:8080/admin-api/park/room-type \
  -H "Content-Type: application/json" -H "Admin-Token: $TOKEN" \
  -d '{"parkCode":"PK_NEW","roomTypeName":"单人间","remark":"样本"}'
curl -s -X POST http://localhost:8080/admin-api/park/room-price \
  -H "Content-Type: application/json" -H "Admin-Token: $TOKEN" \
  -d '{"parkCode":"PK_NEW","price":5000,"priceUnit":"元/月","remark":"样本"}'
```

照护等级 + 照护价格：
```bash
curl -s -X POST http://localhost:8080/admin-api/park/care-type \
  -H "Content-Type: application/json" -H "Admin-Token: $TOKEN" \
  -d '{"parkCode":"PK_NEW","careTypeName":"一级护理","remark":"样本"}'
curl -s -X POST http://localhost:8080/admin-api/park/care-price \
  -H "Content-Type: application/json" -H "Admin-Token: $TOKEN" \
  -d '{"parkCode":"PK_NEW","price":2000,"priceUnit":"元/月","remark":"样本"}'
```

餐饮类型 + 餐饮价格：
```bash
curl -s -X POST http://localhost:8080/admin-api/park/food-type \
  -H "Content-Type: application/json" -H "Admin-Token: $TOKEN" \
  -d '{"parkCode":"PK_NEW","foodTypeName":"普通膳食","remark":"样本"}'
curl -s -X POST http://localhost:8080/admin-api/park/food-price \
  -H "Content-Type: application/json" -H "Admin-Token: $TOKEN" \
  -d '{"parkCode":"PK_NEW","price":800,"priceUnit":"元/月","remark":"样本"}'
```

多媒体图片：
```bash
curl -s -X POST http://localhost:8080/admin-api/park/media-image \
  -H "Content-Type: application/json" -H "Admin-Token: $TOKEN" \
  -d '{"parkCode":"PK_NEW","imageUrl":"/upload/park/sample.jpg","title":"大厅","remark":"样本"}'
```

预期：7 个请求均返回 `code:0`。若有必填字段报错，补字段重试并在报告中记录实际入参。

- [ ] **步骤 7：Step 5 — 机构审核上线（G-10 核心验证）**

```bash
curl -s -X POST "http://localhost:8080/admin-api/park/info/transition?parkCode=PK_NEW&event=approve" \
  -H "Admin-Token: $TOKEN"
```
预期：返回 `data:1`（PARK_SM：0→1 已上线）。

验证 PARK_SM 流转 + is_published 联动（G-12 已实现）：
```bash
docker exec dayan-mysql mysql -uroot -proot dayan -e \
  "SELECT park_code, operate_status, is_published FROM park_info WHERE park_code='PK_NEW'"
```
预期：operate_status=1（已上线）、is_published=1（已实现联动）。

- [ ] **步骤 8：G-10 负向验证（供应商被驳回后机构不能上线）**

新建第二个供应商 SP_BAD，不审核（status=0），尝试为其创建机构并上线：

```bash
# 新建未审核供应商
curl -s -X POST http://localhost:8080/admin-api/supplier/info \
  -H "Content-Type: application/json" -H "Admin-Token: $TOKEN" \
  -d '{"fullName":"测试驳回供应商","supplierType":1,"unifiedCreditCode":"91110108MA01BAD000","contactPerson":"测试","contactPhone":"13900000000"}'
# 记返回的 supplier_code 为 SP_BAD

# 尝试为未审核供应商创建机构（应被 G-9 拦截）
curl -s -X POST http://localhost:8080/admin-api/park/info \
  -H "Content-Type: application/json" -H "Admin-Token: $TOKEN" \
  -d '{"supplierCode":"SP_BAD","fullName":"测试机构","abilityType":1,"province":"北京市","provinceCode":"110000","city":"北京市","cityCode":"110100","district":"海淀区","districtCode":"110108","address":"测试地址","longitude":"116.0","latitude":"39.0"}'
```
预期：第二个请求返回业务错误"供应商未通过审核，无法关联机构: SP_BAD"（G-9 拦截，status=0≠1）。这验证了常量同步生效——若常量仍是 2，未审核供应商 status=0 也会被误判通过。

> 注：G-10 的直接负向验证（机构已创建后，供应商被驳回，再 approve 机构）需要更复杂的数据构造。由于 G-9 已拦截未审核供应商创建机构，G-10 主要防护的是"机构 create 时 supplier 已合作，但 transition(approve) 前 supplier 被暂停"的窄场景。若构造该场景成本高，以 G-9 验证 + 代码审查确认 G-10 逻辑即可，在报告中说明。

- [ ] **步骤 9：回归确认既有 E2E 不受影响**

快速确认既有 TC-E2E-001/002/006 涉及的核心数据未被破坏：
```bash
docker exec dayan-mysql mysql -uroot -proot dayan -e \
  "SELECT order_code, order_status FROM order_equity WHERE order_code IN ('OD202608050013','OD202608050014') ORDER BY id"
```
预期：两条订单状态不变（3/3）。本轮改动不触碰 order/equity/finance 模块，应无影响。

- [ ] **步骤 10：撰写 E2E 报告并追加到文档**

编辑 `docs/test-reports/E2E测试执行报告.md`：

1. 在文件中找到「## 后续待执行用例」表格，将 TC-E2E-005 行的「阻塞」改为「✅ PASS」，并在下方新增 `## TC-E2E-005：供应商入驻→机构上线全流程 ✅ PASS` 章节，结构参照已有 TC-E2E-001/002 章节（含执行结果汇总表、订单/供应商/机构全生命周期、各 Step 详情、G-8 修复验证、G-9/G-10 验证、数据清单表）。

2. 在文末「## 修订记录」表格新增一行：
```
| 2026-08-06 | v1.5 | TC-E2E-005 执行完成（PASS）；修复 G-8（supplier.status 语义混用 bug）；补齐 G-10（机构上线前供应商状态校验）。G-9 常量同步，G-11/G-13 维持现状。五条 E2E 全部通过 |
```

3. 在「## 跨域解耦缺口修复验证」或新增章节中，补充 G-8 的修复说明（参照 G-1/3/5/7 的格式：缺口描述、实现方式、验证结果），并把 G-9（已实现，本轮常量同步）、G-10（本轮补齐）、G-11/G-13（维持现状及理由）一并记录。

报告需包含的实际数据（用前面步骤记录的编码替换占位符）：
- `SP_NEW` = 实际新建供应商编码
- `CT_NEW` = 实际新建合同编码
- `PK_NEW` = 实际新建机构编码
- 各 Step 的 status/audit_status/operate_status/is_published 实际值

- [ ] **步骤 11：Commit E2E 报告**

```bash
cd F:/code/dayan && git add docs/test-reports/E2E测试执行报告.md
git commit -m "test(E2E): TC-E2E-005 供应商入驻→机构上线全流程执行通过

修复 G-8（supplier.status/audit_status 语义混用阻塞性 bug）；
补齐 G-10（机构上线前供应商状态校验）；G-9 常量同步。
五条核心 E2E 全部通过。"
```

---

## 自检结果

**1. 规格覆盖度检查：**

| spec 章节 | 对应任务 |
|-----------|----------|
| §1.3 status 语义修复（常量 + create + audit 两字段分离） | 任务 1 步骤 1-4 |
| §2 G-8（audit 联动） | 任务 1 步骤 3（audit 重写） |
| §2 G-9（常量同步） | 任务 2 步骤 1 |
| §2 G-10（transition 追加校验） | 任务 2 步骤 2 |
| §3.1-3.5 五步接口契约 | 任务 5 步骤 2-7（逐一对应） |
| §4.1 改动清单（2 Service） | 任务 1、任务 2 |
| §4.2 SupplierInfoView 同步注释 | 任务 3 |
| §5 验收标准（bug 修复验证 / G-10 验证 / 五步跑通 / 全模块 BUILD / E2E 报告） | 任务 4（编译）、任务 5 步骤 3/7/8/10（验收点） |
| §6 维持现状（G-11/G-13） | 任务 5 步骤 4/8（观察并记录，不修） |

✅ 无遗漏。

**2. 占位符扫描：** 每个代码步骤含完整代码块或精确 curl 命令。`SP_NEW`/`CT_NEW`/`PK_NEW` 是执行时动态填充的运行时变量（非规格占位符），已明确说明"用前面步骤记录的编码替换"。✅ 无禁止占位符。

**3. 类型一致性检查：**
- `STATUS_PENDING_AUDIT` 任务 1 定义为 `0` → 任务 5 步骤 2 预期 DB `status=0` ✅
- `STATUS_COOPERATING` 任务 1 定义为 `1` → 任务 5 步骤 3 预期 `status=1` ✅
- `AUDIT_PASS=1` / `AUDIT_REJECT=2` → 任务 5 步骤 3 传 `auditStatus:1` ✅，错误提示文案同步改为"1=审核通过 / 2=审核驳回" ✅
- `SUPPLIER_STATUS_APPROVED` 任务 2 改为 `1` → 任务 2 步骤 2 `validateSupplier` 校验 `status==1` 与任务 1 的 `STATUS_COOPERATING=1` 语义一致 ✅
- `validateSupplier(String)` 方法签名未变，任务 2 步骤 2 在 transition 中调用 `validateSupplier(existing.getSupplierCode())` 与已有 create() 调用 `validateSupplier(dto.getSupplierCode())` 签名一致 ✅

✅ 全部一致。
