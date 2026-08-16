SET NAMES utf8mb4;
-- =====================================================================
-- 67_menu_move_knowledge_drop_lecturer.sql  admin 菜单重组
--
-- 1. 知识仓库：资源管理 → 系统管理
--    （百炼知识库属平台级系统配置能力，与素材仓库同级归位；
--     menu_code admin_resource_knowledge → admin_system_knowledge，
--     path /resource/knowledge → /system/knowledge，
--     component resource/knowledge/index → system/knowledge/index，
--     knowledge:repo:* 权限码与后端 /admin-api/knowledge/repo/* 均不变）
-- 2. 课程讲师：下线独立菜单
--    （讲师管理并入「资源管理 → 课程管理」页内抽屉；
--     course:lecturer:* 接口权限保留，抽屉内 CRUD 仍走原接口）
--
-- 种子侧同步：seed/rbac_resource_perm.sql 已移除讲师菜单行，全新初始化不再生成。
-- 知识仓库菜单由 63 号迁移播种，本文件接管归位（63/66 不回改，编号链完整执行）。
-- =====================================================================

-- ---------- 1. 知识仓库迁至系统管理（menu_code 变更，删旧建新） ----------
DELETE FROM `system_menu` WHERE `menu_code` = 'admin_resource_knowledge';
DELETE FROM `organ_role_menu_rel` WHERE `menu_code` = 'admin_resource_knowledge';

INSERT INTO `system_menu`
  (`menu_code`, `menu_name`, `parent_code`, `menu_type`, `path`, `component`, `permission_code`,
   `icon`, `sort_order`, `is_visible`, `domain_type`, `status`, `remark`,
   `created_at`, `updated_at`, `creator`, `updater`, `deleted`)
VALUES
  ('admin_system_knowledge', '知识仓库', 'admin_system', 2, '/system/knowledge', 'system/knowledge/index',
   'knowledge:repo:list', 'Collection', 8, 1, 'admin', 1, '百炼知识仓库管理（平台 + 每渠道一个）',
   NOW(), NOW(), 'system', 'system', 0)
ON DUPLICATE KEY UPDATE `updated_at` = `updated_at`;

-- ---------- 2. 下线「课程讲师」独立菜单（权限码保留） ----------
DELETE FROM `system_menu` WHERE `menu_code` = 'admin_resource_course_lecturer';
DELETE FROM `organ_role_menu_rel` WHERE `menu_code` = 'admin_resource_course_lecturer';
