SET NAMES utf8mb4;
-- =====================================================================
-- 51_system_asset.sql  素材库迁至 system 域（重新定位：系统级文件/外链统一管理）
-- 1. park_asset → system_asset（RENAME 保留全量数据）
-- 2. 新增 storage_type：1=本地OSS 2=外链（存量回填 1）
-- 3. vr_provider 字典 domain：park → system
-- 4. 菜单：资源管理→素材库 迁至 系统管理→素材库（component system/asset/index）
-- 5. 权限：park:asset:* → system:asset:*（角色授权同步平移）
--
-- 链路说明：全新初始化 05 建 park_asset → 47 扩列并插旧菜单 → 本文件统一
-- 改名/迁移收口；47/05 不回改（编号链在全新初始化下仍完整执行）。
-- =====================================================================

RENAME TABLE `park_asset` TO `system_asset`;

ALTER TABLE `system_asset`
  ADD COLUMN `storage_type` TINYINT NOT NULL DEFAULT 1 COMMENT '存储方式（1=本地OSS 2=外链）' AFTER `asset_type`;

UPDATE `system_dict_business`
   SET `domain` = 'system', `updated_at` = NOW()
 WHERE `dict_type` = 'vr_provider' AND `domain` = 'park';

-- 菜单迁移：删旧建新（menu_code 变更；ODKU 兜底可重复执行）
DELETE FROM `system_menu` WHERE `menu_code` = 'admin_resource_asset';
INSERT INTO `system_menu`
  (`menu_code`, `menu_name`, `parent_code`, `menu_type`, `path`, `component`, `permission_code`,
   `icon`, `sort_order`, `is_visible`, `domain_type`, `status`, `remark`,
   `created_at`, `updated_at`, `creator`, `updater`, `deleted`)
VALUES
  ('admin_system_asset', '素材库', 'admin_system', 2, '/system/asset', 'system/asset/index',
   'system:asset:list', 'PictureFilled', 5, 1, 'admin', 1, '系统素材库：OSS 文件与外链统一管理',
   NOW(), NOW(), 'system', 'system', 0)
ON DUPLICATE KEY UPDATE `updated_at` = `updated_at`;

-- 权限迁移：先建新码 → 平移角色授权 → 删旧码
INSERT INTO `organ_permission`
  (`permission_code`, `permission_name`, `parent_code`, `permission_type`,
   `path`, `method`,
   `sort_order`, `status`, `remark`,
   `created_at`, `updated_at`, `creator`, `updater`, `deleted`)
VALUES
  ('system:asset:list',   '素材列表', 'system:asset', 3, '/admin-api/system/asset', 'GET',     210, 1, '系统素材库', NOW(), NOW(), 'system', 'system', 0),
  ('system:asset:query',  '素材详情', 'system:asset', 3, '/admin-api/system/asset/*', 'GET',    211, 1, '系统素材库', NOW(), NOW(), 'system', 'system', 0),
  ('system:asset:create', '新增素材', 'system:asset', 3, '/admin-api/system/asset', 'POST',    212, 1, '系统素材库', NOW(), NOW(), 'system', 'system', 0),
  ('system:asset:update', '修改素材', 'system:asset', 3, '/admin-api/system/asset/*', 'PUT',    213, 1, '系统素材库', NOW(), NOW(), 'system', 'system', 0),
  ('system:asset:delete', '删除素材', 'system:asset', 3, '/admin-api/system/asset/*', 'DELETE', 214, 1, '系统素材库', NOW(), NOW(), 'system', 'system', 0)
ON DUPLICATE KEY UPDATE `updated_at` = `updated_at`;

-- SUBSTRING(permission_code, 11) 取 ':list' 等动作段（'park:asset' 恰为 10 字符）
UPDATE `organ_role_permission_ship`
   SET `permission_code` = CONCAT('system:asset', SUBSTRING(`permission_code`, 11))
 WHERE `permission_code` LIKE 'park:asset:%';

DELETE FROM `organ_permission` WHERE `permission_code` LIKE 'park:asset:%';
