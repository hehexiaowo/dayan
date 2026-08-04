# P4 权益域（核心链路）- 设计规格

> **版本**：v1.0
> **编制日期**：2026-08-04
> **适用范围**：大雁养老 P4 阶段（equity 域，核心商业链路）
> **依据**：`docs/08项目计划书.md` §2.3 P4、P0/P1/P2/P3 已完成基础
> **前置**：P0（状态机引擎 + SnowflakeId + CodeGenerator）、P1（EQUITY_SM 状态机种子 12 条规则）

---

## 一、范围

权益域 6 张表，是连接保险公司与客户的核心商业载体，承载权益全生命周期：

| 序号 | 表 | 类型 | 核心职责 |
|------|----|------|---------|
| 1 | equity_template | 平台共享表（AUTO_INCREMENT） | 权益模板（7 类 + 5 级） |
| 2 | equity_batch | 分片表（雪花ID） | 批次管理（统计字段联动） |
| 3 | **equity_depot** | 分片表 | **核心：权益卡/函库（入库/出库/激活/作废/过期）** |
| 4 | equity_activate | 分片表 | 激活记录（一权益一记录） |
| 5 | equity_use_person | 分片表 | 使用人（≤3，默认权益人） |
| 6 | equity_change_holder | 分片表 | 更换权益人（发起/完成/回滚） |

### P4 本阶段（必做）
- 6 表 Service + Controller CRUD
- **EQUITY_SM 8 态状态机全链路接入**（12 条规则，已存在于种子）
- 批量入库（批量生成权益记录）、出库、激活、作废、过期扫描
- 激活码 DY-8 位 / 绑定码 BF-12 位生成与验证
- 批次统计字段联动（produced/allocated/outbound/activated/used/expired/voided/remain）
- 使用人 ≤3 + 同身份证号唯一（应用层） + 默认权益人标记
- 更换权益人发起/完成/回滚
- 过期扫描定时任务（dayan-job 接入 EquityDepotMapper）

### P4 后置（不在本阶段）
- Admin 前端页面（P8）
- 二维码生成（qr_code_url 实际生成，P4 只存字段）
- 实名认证/协议签署外部对接（P4 只存标记）
- 权益与订单关联的完整结算（P7 订单/结算域）

---

## 二、全局约束（实现者必读）

### 2.1 包结构
- `com.dayan.equity.{controller,service,service.impl,dto,vo,converter,enums,statemachine}`
- Controller 仅 `controller/admin`（Admin 端管理）。P4 不做 client/agent 端激活接口（那是 P10 前端阶段，本阶段激活由 Admin 端或 OpenAPI 触发均可，统一走 admin-api）。

### 2.2 分片表主键策略（重要裁定）
DDL 中 5 张分片表（batch/depot/activate/use_person/change_holder）主键为 `BIGINT NOT NULL`（**无 AUTO_INCREMENT**），注释为"雪花ID"。但 P0 代码生成的 Entity 统一标了 `@TableId(type = IdType.AUTO)`——**这对分片表是错误的**（DB 无自增，insert 会失败）。

**裁定（P4 必须执行）**：将 5 张分片表的 Entity 主键改为 `@TableId(type = IdType.ASSIGN_ID)`：
- `IdType.ASSIGN_ID` = MyBatis-Plus 内置雪花算法，insert 前自动填充 Long 型 ID，无需手动 set，无需额外 Bean。
- **仅改 equity 域 5 个分片表 Entity**（EquityBatch/EquityDepot/EquityActivate/EquityUsePerson/EquityChangeHolder）。
- equity_template 保持 `IdType.AUTO`（平台共享表，DDL 有 AUTO_INCREMENT）。
- P2 的 agent/client 分片表同样问题，**本阶段不修**（P2 未运行，留待统一治理；仅在账本记录此技术债）。

### 2.3 编码生成（CodeGenerator Bean）
- equity_code：前缀 `EQ` + 12 位（`BusinessCode.EQUITY`，规格要求 EQ+12）。CodeGenerator 默认宽度 5，需用 `codeGenerator.generate("EQ")` 但宽度不够。**裁定**：equity_code 用 `codeGenerator.generate(BusinessCode.EQUITY)` 生成（宽度 5 不足时自然扩展为 EQ+12 不保证，需特殊处理）。

  **实际方案**：equity_code 不走 CodeGenerator（它只支持固定宽度补零）。改为：
  - 用 `SnowflakeId` 生成纯数字（13 位毫秒时间戳基础上），再前缀 "EQ" + 补齐到 12 位数字。
  - 或更简单：`"EQ" + String.format("%012d", snowflakeId.nextId() % 1_000_000_000_000L)`——但 SnowflakeId 不是 Bean。
  - **最终裁定（最简）**：equity_code = `"EQ" + System.currentTimeMillis() + 随机3位`，12 位数字部分由时间戳(13位取后10位)+随机3位组成。**并发安全由 DB 唯一键 `uk_equity_code` 兜底**（冲突重试）。或者复用 SequenceProvider：注入 `SequenceProvider`，`"EQ" + String.format("%012d", sequenceProvider.next("code:seq:EQ:0"))`。

  **P4 实现者选用 SequenceProvider 方案**（与 CodeGenerator 内部一致，Redis INCR 并发安全）：
  ```java
  String equityCode = "EQ" + String.format("%012d", sequenceProvider.next("code:seq:EQ:0"));
  ```
  equity_no（对外展示卡号）同规则但独立序列：`"DY" + format(...)` 或与 equity_code 相同——**裁定：equity_no = equity_code**（同一值，简化；uk_equity_no 唯一键也满足）。

- batch_code：前缀 `BC` + 8 位，`"BC" + String.format("%08d", sequenceProvider.next("code:seq:BC:0"))`
- template_code：前缀 `ET` + 5 位，同上
- activate_code（激活记录编码，非卡激活码）：`"AC" + format(...)`
- activate_code（**权益卡的激活码，DY-8 位**）：`"DY" + 随机8位数字`（不连续，防猜测；生成后存 equity_depot.activate_code）。**并发安全由应用层查重 + DB 兜底**。
- bind_code（**权益函的绑定码，BF-12 位**）：`"BF" + 随机12位数字`。

  **随机码生成**：用 `ThreadLocalRandom.current().ints(8, 0, 10)...` 拼数字，查重循环最多 5 次。

### 2.4 状态机（EQUITY_SM，8 态）
- domain 参数 = `"EQUITY_SM"`（machineCode）
- 状态字段 = equity_depot.`equity_status`，8 态：`0=库存中 / 1=已出库 / 2=已激活 / 3=使用中 / 4=已完成 / 5=已过期 / 6=已作废 / 7=更换权益人中`
- 12 条规则（已存在于 `db/migration/seed/state_machine_seed.sql`）：
  - `outbound`: 0→1（出库）
  - `activate`: 1→2（激活）
  - `start_service`: 2→3（发起服务）
  - `end_service`: 3→2（服务结束恢复）
  - `complete`: 3→4（权益完成）
  - `shelf_expire`: 0→5（上架过期）
  - `expire`: 1→5 / 2→5（有效期过期）
  - `void`: 0→6 / 1→6（作废，**注意：2已激活不能直接作废**）
  - `change_holder`: 2→7（发起更换权益人）
  - `change_done`: 7→2（更换完成）
- **所有 equity_status 变更必须经 `stateMachineEngine.transition("EQUITY_SM", from, event)`**，返回 to 后落库。非法转移抛 BusinessException（引擎内自动抛）。
- 引擎 Bean 已由 dayan-common-redis 提供，注入 `private final StateMachineEngine stateMachineEngine;`。规则由 system 模块启动时 loadAllRules 加载（方案 B，同 P3 park）。

### 2.5 租户隔离（重要裁定）
- **现状**：`equity_` 不在 DayanTenantHandler 忽略清单（DEFAULT_IGNORE_PREFIXES 仅 system_/organ_/butler_/distributor_）。equity_depot 等表有 channel_code 字段，会被自动追加 `channel_code = ?` 隔离。
- **问题**：当 Admin 端 ContextHolder.channel_code 为空时，getTenantId() 返回 `LongValue(0L)`，导致查询追加 `channel_code = 0`，**查不到任何数据**（实际 channel_code 是 "CH00001" 字符串）。阻断核心链路。
- **裁定（P4 必须执行）**：将 `equity_` 加入 DayanTenantHandler 的 DEFAULT_IGNORE_PREFIXES。
  - 改 `dayan-common-mybatis/.../DayanTenantHandler.java`，DEFAULT_IGNORE_PREFIXES 数组追加 `"equity_"`。
  - 这样 equity 全域 6 表不参与自动租户隔离，Admin 端全局可见；渠道归属通过 equity_depot.channel_code 字段**显式条件查询**（需要按渠道过滤时手动 `.eq(channel_code)`）。
- **实现者**：此项由 P4-A 任务顺带完成（改一行常量数组），无需独立任务。

### 2.6 批次统计字段联动（关键业务逻辑）
equity_batch 有 8 个统计字段，**任何 equity_depot 状态变更都要联动更新批次统计**（事务内）：
- 入库(produced)：batch.produced_count += N, remain_count += N
- 出库(outbound)：batch.outbound_count += N, remain_count -= N（remain = produced - outbound - voided - expired... 实际 remain = 库存中数量）
- 激活(activate)：batch.activated_count += 1
- 作废(void)：batch.voided_count += 1, remain_count -= 1
- 过期(expire/shelf_expire)：batch.expired_count += 1, remain_count -= 1
- 使用(complete)：batch.used_count += 1

**简化实现**：每次操作后，按 equity_code 找到 batch_code，对 batch 对应统计字段 +1（或批量操作时 +N），用 `UPDATE equity_batch SET xxx_count = xxx_count + N WHERE batch_code = ?`（乐观锁或直接 SQL 增量，避免读-改-写竞态）。

### 2.7 统一返回 / 异常 / DTO
- Controller 返回 `R<T>` / `R<PageResult<T>>`
- 异常 `BusinessException` + `ErrorCode`
- DTO→Entity→VO 手动 `BeanUtils.copyProperties`（同 P2/P3）
- Service `@RequiredArgsConstructor` 注入 Mapper

### 2.8 参考实现
- `dayan-module-channel` Service+Controller 模式（P2）
- `dayan-module-park` 状态机接入（P3，方案 B）
- `dayan-module-system` 的 SystemStateRuleLoader（若需自建 loader，但方案 B 不需要）
- `com.dayan.common.redis.RedisSequenceProvider`（SequenceProvider 实现，Redis INCR）

---

## 三、各表设计

### 3.1 equity_template（权益模板）
- CRUD + template_code 唯一校验
- 7 类权益（equity_type: 1机构入住/2机构参观/3场景活动/4居家护理/5健康检测/6课程学习/7旅居体验）
- 5 级等级（equity_level: 1基础/2标准/3高级/4尊享/5定制）
- valid_days（激活后有效天数）+ shelf_life_days（库存有效期天数）+ max_use_count
- 编码 ET+5（SequenceProvider）
- status: 0停用/1启用/2已下架

### 3.2 equity_batch（批次管理）
- CRUD + batch_code 唯一
- 关联 template_code（校验模板存在且 status=1）
- 关联 channel_code（可空，分配渠道）
- total_quantity / produced_count 等 8 统计字段（见 §2.6 联动）
- batch_status: 0待生产/1生产中/2已完成/3已出库/4已关闭
- 日期校验：produce_date < expire_date
- **outbound ≤ total 校验**：出库操作时校验 batch.outbound_count + 本次数量 ≤ total_quantity

### 3.3 equity_depot（核心：权益卡/函库）
这是 P4 最复杂的表，承载核心链路。关键操作：

**a) 批量入库（stockIn）**：
- 入参：batch_code, quantity, carrier_type(1卡/2函)
- 逻辑：循环 quantity 次，生成 equity_code(EQ+12)、equity_no(同)、activate_code(DY-8,卡)/bind_code(BF-12,函)、shelf_expire_time(=produce_time + template.shelf_life_days)、card_secret(AesGcmUtil 加密的随机串)、equity_status=0、equity_type/value/cost_price 从 template 冗余
- **批量插入**（MyBatis-Plus `saveBatch` 或 mapper.insert 循环）
- 联动 batch.produced_count += quantity, remain_count += quantity
- 批次状态：若 batch_status=0 待生产 → 1 生产中（首次入库）→ 全部入库后 2 已完成（produced==total）

**b) 出库（outbound）**：
- 入参：equity_code 列表（或 batch_code + 数量自动选未出库的）, outbound_channel_code/outbound_agent_code, logistics_no
- 逻辑：对每张权益，校验 equity_status=0（库存中），经状态机 `transition("EQUITY_SM",0,"outbound")`→1，写 outbound_time/channel/agent/logistics_no
- 联动 batch.outbound_count += N, remain_count -= N

**c) 激活（activate）**：
- 入参：activate_code(卡)或bind_code(函) + client_code + client_phone + client_full_name + activate_channel + activate_source_code + ip/device
- 逻辑：
  1. 按 activate_code/bind_code 查 equity_depot，校验存在且 equity_status=1（已出库，未激活）。若 0 库存中不能直接激活（须先出库）——**裁定：允许 0→1→2 严格走，不允许 0→2 跳跃**。
  2. 经状态机 `transition("EQUITY_SM",1,"activate")`→2
  3. 写 activate_time, expire_time = activate_time + template.valid_days（查 template）, client_code
  4. 插 equity_activate 记录（一权益一记录，uk_activate_code 用 AC+序列）
  5. 联动 batch.activated_count += 1
  6. **自动创建默认使用人**：equity_use_person 插一条（client_code 持有人，use_person_name=client_full_name，relation=本人，is_default_holder=1）
- **事务**：整个操作 `@Transactional`，保证原子性

**d) 作废（void）**：
- 入参：equity_code + void_reason
- 校验 equity_status ∈ {0,1}（库存中/已出库可作废；已激活 2 不能直接作废——状态机不允许）
- 经状态机 `transition("EQUITY_SM",from,"void")`→6
- 写 void_reason
- 联动 batch.voided_count += 1, remain_count -= 1（若 from=0）

**e) 更换权益人（changeHolder）**：
- 发起：校验 equity_status=2，`transition(...,"change_holder")`→7，插 equity_change_holder 记录(change_status=0待处理)，校验同一权益无 change_status=0 的在途记录
- 完成：校验 change 记录存在且 0，`transition(...,"change_done")`→2，更新 change_status=1，切换 equity_use_person 的 is_default_holder（旧的置0，新的置1；若新使用人不存在则先建）
- 回滚：change_status 0→2，equity_status 7→2（注意：状态机无 7→2 的 rollback 事件，**裁定用 change_done 事件回到 2**，语义上回滚=权益恢复原持有人）

### 3.4 equity_activate（激活记录）
- 一权益一记录（uk_activate_code 是激活记录编码唯一，非 equity_code 唯一——**裁定：应用层校验同一 equity_code 仅一条激活记录**）
- 由 activate 操作自动产生（见 3.3c），**不单独提供新增接口**，仅查询

### 3.5 equity_use_person（使用人）
- 登记使用人：校验同 equity_code 下使用人 ≤3
- 同身份证号唯一（应用层，按 equity_code + use_person_id_card 查重；id_card 加密存储用 AesGcmUtil）
- is_default_holder 唯一（同 equity_code 仅1个默认，设新默认时旧置0）
- 支持更换默认权益人接口

### 3.6 equity_change_holder（更换权益人记录）
- 由 changeHolder 操作产生/更新（见 3.3e）
- 查询接口（按 equity_code 查更换历史）
- change_status: 0待处理/1已完成/2已回滚

---

## 四、定时任务（dayan-job 接入）

### 4.1 过期扫描（EquityExpireScheduler）
- 现有占位：`dayan-server/dayan-job/.../EquityExpireScheduler.java`，每小时 `@Scheduled(cron = "0 0 * * * ?")`
- **P4 改造**：
  1. dayan-job/pom.xml 加依赖 `dayan-module-equity`
  2. 注入 `EquityDepotMapper`（或 EquityDepotService）
  3. 扫描两类过期：
     - shelf_expire：`equity_status=0 AND shelf_expire_time < NOW()` → 状态机 `transition("EQUITY_SM",0,"shelf_expire")`→5，联动 batch.expired_count
     - expire：`equity_status IN (1,2) AND expire_time < NOW()` → `transition("EQUITY_SM",from,"expire")`→5，联动 batch
  4. 分批处理（每次 500 条），避免大事务
- **注意**：状态机引擎在 dayan-job 进程也能用（dayan-common-redis 提供 Bean），规则已由 system 模块加载到 Redis（共享）。但 dayan-job 若不依赖 system 模块，Redis 规则仍在（缓存独立于进程）。**dayan-job 依赖 equity 模块即可**（equity 依赖 common-redis）。

### 4.2 性能要求（验收）
- 批量入库 1000 张 < 5 秒
- 激活接口 P99 < 200ms
- 编码生成 1000 并发不重复（SequenceProvider Redis INCR 保证）

---

## 五、API 路径规范

统一 Admin 端（context-path=/admin-api 拼接）：

| 子域 | 路径 | 关键动作 |
|------|------|---------|
| 模板 | `/equity/template` | `/page /list /{code} POST / PUT / DELETE /{code}` |
| 批次 | `/equity/batch` | `/page /{code} POST / PUT / DELETE /{code}` + `/stats/{code}`(查统计) |
| 卡库 | `/equity/depot` | `/page /{code}` + `POST /stock-in`(批量入库) + `POST /outbound`(出库) + `POST /activate`(激活) + `POST /void`(作废) + `POST /change-holder`(发起换人) + `POST /change-done`(完成换人) + `POST /change-rollback`(回滚) |
| 激活记录 | `/equity/activate` | `/page /{equityCode}`(查某权益激活记录) |
| 使用人 | `/equity/use-person` | `/page /list-by-equity /{id} POST / PUT / DELETE /{id}` + `POST /set-default`(设默认) |
| 换人记录 | `/equity/change-holder` | `/page /list-by-equity /{equityCode}` |

---

## 六、验收标准

| 维度 | 标准 |
|------|------|
| 模板/批次 | CRUD + 模板编码唯一 + 批次统计联动 |
| 核心链路 | 入库(批量)→出库→激活→使用→完成/过期/作废 全态流转经状态机 |
| 使用人 | ≤3 限制 + 身份证唯一 + 默认权益人唯一 |
| 换人 | 发起(2→7)/完成(7→2)/回滚 全流程 |
| 过期扫描 | 定时任务接入，shelf_expire + expire 两类 |
| 状态机 | 8 态 12 规则，非法转移抛异常 |
| 分片表主键 | 5 分片表 ASSIGN_ID 正常 insert |
| 编译 | 41 模块 BUILD SUCCESS |

---

## 七、任务拆分与执行

equity 域 6 表强耦合（depot 是核心，其余围绕它），**不宜像 P3 那样三路并行**（共享 equity_depot 逻辑会冲突）。改为**串行+小并行**：

| 任务 | 内容 | 依赖 |
|------|------|------|
| **P4-A** | 模板 + 批次（2 表）CRUD + 批次统计基础 | 无 |
| **P4-B** | depot 核心链路（入库/出库/激活/作废）+ 状态机接入 + 分片表 ASSIGN_ID 改造 | P4-A（依赖 template 查询） |
| **P4-C** | 激活记录(查) + 使用人(≤3/默认/换人) + 更换权益人记录 + 换人操作(发起/完成/回滚) | P4-B（依赖 depot 状态机） |
| **P4-D** | 过期扫描定时任务接入（dayan-job） | P4-B（依赖 depot mapper/service） |

**执行策略**：P4-A 先做（独立），完成后 P4-B 和 P4-C 有依赖关系但操作不同文件可部分并行（B 做 depot，C 做 use_person/change_holder），P4-D 最后。
**建议**：P4-A 单独做 → P4-B+C 并行（B 改 depot+batch 联动，C 做 activate/use_person/change_holder 的 CRUD 和换人）→ P4-D。
