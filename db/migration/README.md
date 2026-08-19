# db/migration 数据库迁移目录规约

本目录整体挂载为 MySQL 容器的 `/docker-entrypoint-initdb.d`，**仅用于全新初始化**。
官方 entrypoint 按字典序执行 `.sql` / `.sh`：`01~90` 编号 SQL → `99_seed.sh`（后者遍历 `seed/*.sql`）。

## 铁律

1. **所有 `.sql` 文件必须以 `SET NAMES utf8mb4;` 开头**（本文件同目录下任一文件可复制头部）。
   历史事故：手动 `source` 无此声明的文件会导致中文 COMMENT 双重编码乱码（参见
   `scripts/fix-*-mojibake.sql` 留档）。新增文件缺少该行 = review 不通过。
2. **编号只增不改**：新变更一律追加新编号文件（下一个为 `91_`），已入库文件不回改；
   已执行过的修复结果需要沉淀时，同步回填对应基础 DDL 文件并保证编号链在
   全新初始化下仍可完整执行。
3. **种子数据必须幂等**：`seed/*.sql` 的 INSERT 一律带
   `ON DUPLICATE KEY UPDATE`（空操作守卫用 `` `updated_at` = `updated_at` ``；
   **禁止 `id = id`**——ODKU 写自增列在 MySQL 8 会抛 1869）。
   保证重复执行不报错。
4. **一次性脚本/过期重构/ETL 工具不进本目录**：
   - 已折叠进基础 DDL 的历史重构 → `db/archive/`
   - 一次性数据修复、ETL 工具与数据源 → `db/tools/`
5. **演示/测试账号不进自动初始化链路**：`seed/rbac_demo_seed.sql` 已被
   `99_seed.sh` 排除，仅手动执行，严禁在生产环境执行。

## 目录边界

| 位置 | 内容 |
|---|---|
| `db/migration/*.sql` | 编号迁移（DDL + 必要的数据迁移），按编号顺序执行 |
| `db/migration/seed/` | 种子数据（菜单/字典/区域/状态机/RBAC 权限等），幂等 |
| `db/migration/99_seed.sh` | seed 目录执行器（排除 rbac_demo_seed.sql） |
| `db/archive/` | 已过期重构脚本与一次性脚本（留档，不执行） |
| `db/tools/` | 老系统数据迁移 ETL（Python）与 TSV 数据源（手动使用） |
