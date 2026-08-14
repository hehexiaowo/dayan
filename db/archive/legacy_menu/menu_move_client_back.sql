-- =====================================================================
-- menu_move_client_back.sql  客户管理从权益管理移回渠道管理 + 渠道目录排序调整
--
-- 变更内容：
--   1. 客户管理：权益管理 → 渠道管理
--      menu_code admin_equity_client → admin_channel_client
--      path/component equity/client → channel/client，parent admin_equity → admin_channel
--   2. 渠道管理目录下 4 个子菜单重新排序：
--      分销管理(1) → 渠道列表(2) → 队伍管理(3) → 客户管理(4)
--
-- 安全性 / FK 处理：
--   - menu_code 是 system_menu 主键，被 organ_role_menu_rel.menu_code 外键引用。
--     ROLE_OPERATOR 演示角色授权了 admin_equity_client，改 PK 前必须先更新子表 rel。
--   - 前端页面目录已迁移 dayan-admin/src/views/equity/client → channel/client。
--   - 后端无需改动（数据驱动）。
--
-- 执行方式：对运行中的 MySQL 执行（docker exec）。
-- 幂等：按精确 menu_code 定位，重复执行无副作用。
-- =====================================================================
SET NAMES utf8mb4;
SET @old_fk = @@SESSION.foreign_key_checks;
SET SESSION foreign_key_checks = 0;

-- ============================================================
-- 第 0 步：先同步 organ_role_menu_rel（外键子表）
-- ============================================================
UPDATE `organ_role_menu_rel`
SET `menu_code` = 'admin_channel_client'
WHERE `menu_code` = 'admin_equity_client';

-- ============================================================
-- 第 1 步：客户管理移回渠道管理（改 code+path+component+parent）
-- ============================================================
UPDATE `system_menu`
SET `menu_code` = 'admin_channel_client', `parent_code` = 'admin_channel',
    `path` = '/channel/client', `component` = 'channel/client/index',
    `sort_order` = 4, `updated_at` = NOW()
WHERE `menu_code` = 'admin_equity_client' AND `domain_type` = 'admin';

-- ============================================================
-- 第 2 步：渠道管理目录下子菜单重新排序
--   分销管理(1) → 渠道列表(2) → 队伍管理(3) → 客户管理(4，已在上步设置)
-- ============================================================
UPDATE `system_menu` SET `sort_order` = 1, `updated_at` = NOW()
WHERE `menu_code` = 'admin_channel_distributor' AND `domain_type` = 'admin';

UPDATE `system_menu` SET `sort_order` = 2, `updated_at` = NOW()
WHERE `menu_code` = 'admin_channel_info' AND `domain_type` = 'admin';

UPDATE `system_menu` SET `sort_order` = 3, `updated_at` = NOW()
WHERE `menu_code` = 'admin_channel_agent' AND `domain_type` = 'admin';

SET SESSION foreign_key_checks = @old_fk;
