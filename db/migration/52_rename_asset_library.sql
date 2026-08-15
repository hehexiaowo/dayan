SET NAMES utf8mb4;
-- =====================================================================
-- 52_rename_asset_library.sql  素材库 → 素材仓库（与机构详情顶层 tab 命名对齐）
-- 1. 菜单名：系统管理 → 素材仓库（menu_code/path/权限码不变，仅展示名）
-- 2. system:asset:* 权限备注：系统素材库 → 系统素材仓库
-- 说明：51 建行时为旧名「素材库」，本文件统一收口；全新初始化链
--       51 插旧名 → 本文件改名 → 种子（已为终态）no-op，两条路径收敛。
-- =====================================================================

UPDATE `system_menu`
   SET `menu_name` = '素材仓库',
       `remark` = '系统素材仓库：OSS 文件与外链统一管理',
       `updated_at` = NOW()
 WHERE `menu_code` = 'admin_system_asset';

UPDATE `organ_permission`
   SET `remark` = '系统素材仓库',
       `updated_at` = NOW()
 WHERE `permission_code` LIKE 'system:asset:%'
   AND `remark` = '系统素材库';
