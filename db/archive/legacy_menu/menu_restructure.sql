-- =====================================================================
-- menu_restructure.sql  Admin 端菜单目录重组
--
-- 三项结构调整：
--   1. 新建「基础管理」目录(admin_basic, sort=5，紧跟首页看板)，从系统管理迁出
--      账号/角色/菜单/架构 4 个子菜单，menu_code 前缀 system→basic、path/component 同步。
--      原「系统管理」目录保留，装剩下的字典/状态规则/系统配置/操作日志。
--   2. 分销管理：资源管理 → 渠道管理（menu_code admin_resource_distributor→admin_channel_distributor，
--      path/component resource/distributor→channel/distributor）。
--   3. 客户管理：渠道管理 → 权益管理（menu_code admin_channel_client→admin_equity_client，
--      path/component channel/client→equity/client）。
--
-- 前端页面目录已同步迁移（basic/{account,role,menu,organ}、channel/distributor、equity/client）。
--
-- 安全性 / FK 处理：
--   - menu_code 是 system_menu 主键，被 organ_role_menu_rel.menu_code 外键引用。
--     改 PK 值前必须先更新子表 organ_role_menu_rel，否则 FK 约束报错。
--     本脚本先 UPDATE organ_role_menu_rel，再 UPDATE system_menu。
--   - ROLE_OPERATOR 演示角色授权了 admin_channel_client（移到权益后改 admin_equity_client），
--     后端 withAncestors 会自动补全新父目录 admin_equity，侧边栏层级不丢。
--   - 其余 5 个被改 code 的菜单无任何 role-menu rel 引用，零级联风险。
--
-- 执行方式：对运行中的 MySQL 执行（docker exec）。
-- 幂等：按精确 menu_code 定位，重复执行无副作用。
-- =====================================================================
SET NAMES utf8mb4;
SET @old_fk = @@SESSION.foreign_key_checks;
SET SESSION foreign_key_checks = 0;

-- ============================================================
-- 第 0 步：先同步 organ_role_menu_rel（外键子表），避免 PK 改动时 FK 报错
-- ============================================================
UPDATE `organ_role_menu_rel`
SET `menu_code` = 'admin_equity_client'
WHERE `menu_code` = 'admin_channel_client';

-- ============================================================
-- 第 1 步：新建「基础管理」目录
-- ============================================================
INSERT INTO `system_menu`
  (`menu_code`, `menu_name`, `parent_code`, `menu_type`, `path`, `component`, `permission_code`,
   `icon`, `sort_order`, `is_visible`, `domain_type`, `status`, `remark`,
   `created_at`, `updated_at`, `creator`, `updater`, `deleted`)
VALUES
  ('admin_basic', '基础管理', NULL, 1, '/basic', NULL, NULL,
   'Setup', 5, 1, 'admin', 1, '基础管理目录（账号/角色/菜单/架构）',
   NOW(), NOW(), 'system', 'system', 0)
ON DUPLICATE KEY UPDATE
  `menu_name` = VALUES(`menu_name`), `parent_code` = VALUES(`parent_code`),
  `menu_type` = VALUES(`menu_type`), `path` = VALUES(`path`),
  `sort_order` = VALUES(`sort_order`), `is_visible` = VALUES(`is_visible`),
  `domain_type` = VALUES(`domain_type`), `status` = VALUES(`status`),
  `remark` = VALUES(`remark`), `updated_at` = NOW();

-- ============================================================
-- 第 2 步：账号/角色/菜单/架构 4 子菜单迁到基础管理（改 code+path+component+parent）
-- ============================================================
-- 账号管理
UPDATE `system_menu`
SET `menu_code` = 'admin_basic_account', `parent_code` = 'admin_basic',
    `path` = '/basic/account', `component` = 'basic/account/index',
    `sort_order` = 1, `updated_at` = NOW()
WHERE `menu_code` = 'admin_system_account' AND `domain_type` = 'admin';

-- 角色管理
UPDATE `system_menu`
SET `menu_code` = 'admin_basic_role', `parent_code` = 'admin_basic',
    `path` = '/basic/role', `component` = 'basic/role/index',
    `sort_order` = 2, `updated_at` = NOW()
WHERE `menu_code` = 'admin_system_role' AND `domain_type` = 'admin';

-- 菜单管理
UPDATE `system_menu`
SET `menu_code` = 'admin_basic_menu', `parent_code` = 'admin_basic',
    `path` = '/basic/menu', `component` = 'basic/menu/index',
    `sort_order` = 3, `updated_at` = NOW()
WHERE `menu_code` = 'admin_system_menu' AND `domain_type` = 'admin';

-- 组织架构
UPDATE `system_menu`
SET `menu_code` = 'admin_basic_organ', `parent_code` = 'admin_basic',
    `path` = '/basic/organ', `component` = 'basic/organ/index',
    `sort_order` = 4, `updated_at` = NOW()
WHERE `menu_code` = 'admin_system_organ' AND `domain_type` = 'admin';

-- ============================================================
-- 第 3 步：分销管理 资源管理→渠道管理
-- ============================================================
UPDATE `system_menu`
SET `menu_code` = 'admin_channel_distributor', `parent_code` = 'admin_channel',
    `path` = '/channel/distributor', `component` = 'channel/distributor/index',
    `sort_order` = 3, `updated_at` = NOW()
WHERE `menu_code` = 'admin_resource_distributor' AND `domain_type` = 'admin';

-- ============================================================
-- 第 4 步：客户管理 渠道管理→权益管理
-- ============================================================
UPDATE `system_menu`
SET `menu_code` = 'admin_equity_client', `parent_code` = 'admin_equity',
    `path` = '/equity/client', `component` = 'equity/client/index',
    `sort_order` = 4, `updated_at` = NOW()
WHERE `menu_code` = 'admin_channel_client' AND `domain_type` = 'admin';

-- ============================================================
-- 第 5 步：重排系统管理剩余子菜单的 sort_order（1-4 连续）
-- ============================================================
UPDATE `system_menu` SET `sort_order` = 1, `updated_at` = NOW()
WHERE `menu_code` = 'admin_system_dict' AND `domain_type` = 'admin';
UPDATE `system_menu` SET `sort_order` = 2, `updated_at` = NOW()
WHERE `menu_code` = 'admin_system_state_machine' AND `domain_type` = 'admin';
UPDATE `system_menu` SET `sort_order` = 3, `updated_at` = NOW()
WHERE `menu_code` = 'admin_system_config' AND `domain_type` = 'admin';
UPDATE `system_menu` SET `sort_order` = 4, `updated_at` = NOW()
WHERE `menu_code` = 'admin_system_log' AND `domain_type` = 'admin';

SET SESSION foreign_key_checks = @old_fk;
