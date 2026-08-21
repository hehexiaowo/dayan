# db/migration 数据库初始化目录规约

本目录整体挂载为 MySQL 容器的 `/docker-entrypoint-initdb.d`，**仅用于全新初始化**。
官方 entrypoint 按字典序执行 `.sql`：`schema.sql`（建表）→ `seed.sql`（种子数据）。

## 结构

| 文件 | 内容 |
|---|---|
| `schema.sql` | **最终数据库架构**：140 张表全部 `CREATE TABLE IF NOT EXISTS` |
| `seed.sql` | **最终数据库种子**：96 张表 8232 条 `INSERT IGNORE`（幂等） |

历史编号迁移链（01~94 + 99_seed.sh + seed/）已于 2026-08-21 合并为上述两文件并删除
（git 历史可查），老系统 ETL 工具与旧备份同步清理。
合并工具与全量导出源：`db/tools/merge_schema_seed.py` + `db/backup_dayan_20260821.sql`。

## 铁律

1. **所有 `.sql` 文件必须以 `SET NAMES utf8mb4;` 开头**（本文件同目录下任一文件可复制头部）。
   历史事故：手动 `source` 无此声明的文件会导致中文 COMMENT 双重编码乱码（参见
   `scripts/fix-*-mojibake.sql` 留档）。新增文件缺少该行 = review 不通过。
2. **架构/种子直接改本文件，不再新增编号迁移链**：数据库已基本稳定，变更通过 git
   版本化；如确需一次性变更脚本，以 `9x_xxx.sql` 追加（字典序保证排在 schema/seed
   之后执行），完成后及时折叠回 schema/seed 并删除。
3. **种子数据必须幂等**：`seed.sql` 的 INSERT 一律为 `INSERT IGNORE INTO`，
   保证重复执行不报错、不覆盖已存在行。
4. **一次性脚本/过期重构不进本目录**：一次性数据修复与 ETL 工具 → `db/tools/`，
   用后即删（git 历史可查），不做长期留档。
5. **演示/测试数据**：以 2026-08-21 全量导出为基准，演示账号（operator/op123、
   ROLE_OPERATOR 及其菜单/权限关联）已并入 `seed.sql`；如需在生产环境剔除，
   编辑 `seed.sql` 中 `organ_account`/`organ_role`/`organ_account_role_rel`/
   `organ_role_menu_rel`/`organ_permission`/`organ_role_permission_ship`
   六张表的演示行。

## 目录边界

| 位置 | 内容 |
|---|---|
| `db/migration/schema.sql` | 最终架构（140 表） |
| `db/migration/seed.sql` | 最终种子（96 表，幂等） |
| `db/tools/` | 合并工具 merge_schema_seed.py（可复现 schema/seed 生成） |
| `db/backup_dayan_20260821.sql` | 合并基准：全量导出源（merge 脚本输入，勿删） |
