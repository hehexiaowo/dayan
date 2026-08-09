-- =====================================================================
-- menu_rename.sql  Admin 端菜单改名 + 用户管理→账号管理标识符迁移
--
-- 变更内容（3 条菜单）：
--   1. 用户管理 → 账号管理
--      连标识符一起改：menu_code admin_system_user → admin_system_account
--      path /system/user → /system/account，component system/user/index → system/account/index
--      （permission_code 已是 organ:account:list，无需动）
--   2. 供货管理 → 供应管理（仅显示名，标识符 supplier 不变）
--   3. 代理管理 → 队伍管理（仅显示名，标识符 agent 不变；remark 代理人管理→队伍管理）
--
-- 安全性：
--   - admin_system_user 无任何 organ_role_menu_rel 引用（超管 is_admin 运行期绕过授权），
--     改 menu_code 主键值无级联风险；运营演示角色 ROLE_OPERATOR 也未授予该菜单。
--   - /system/account 在 admin 域无冲突（channel 域的 channel_system_account 是独立 domain_type，不混）。
--   - 前端目录 dayan-admin/src/views/system/user → system/account 同步改名，
--     动态路由 resolveComponent('system/account/index') 解析到新路径。
--
-- 执行方式：当前库已存在，对运行中的 MySQL 执行（docker exec）。
-- 幂等：按 menu_code 精确定位，重复执行无副作用。
-- =====================================================================
SET NAMES utf8mb4;

-- ========== 1. 用户管理 → 账号管理（含标识符迁移）==========
UPDATE `system_menu`
SET `menu_code`   = 'admin_system_account',
    `menu_name`   = '账号管理',
    `path`        = '/system/account',
    `component`   = 'system/account/index',
    `updated_at`  = NOW()
WHERE `menu_code` = 'admin_system_user'
  AND `domain_type` = 'admin';

-- ========== 2. 供货管理 → 供应管理（仅显示名）==========
UPDATE `system_menu`
SET `menu_name`  = '供应管理',
    `updated_at` = NOW()
WHERE `menu_code` = 'admin_resource_supplier'
  AND `domain_type` = 'admin';

-- ========== 3. 代理管理 → 队伍管理（显示名 + remark 对齐）==========
UPDATE `system_menu`
SET `menu_name`  = '队伍管理',
    `remark`     = '队伍管理',
    `updated_at` = NOW()
WHERE `menu_code` = 'admin_channel_agent'
  AND `domain_type` = 'admin';
