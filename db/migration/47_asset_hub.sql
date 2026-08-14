SET NAMES utf8mb4;
-- =====================================================================
-- 47_asset_hub.sql  素材库统一收口（规格 §A）
-- 1. park_code 允许 NULL（NULL=平台素材：内容封面/课程视频/商品图等）
-- 2. asset_url 索引：幂等登记查询 + 删除引用校验加速
-- 3. VR 提供商字典化（vr_provider 自由文本 → 字典）
-- 4. 全局素材库菜单（复用 park:asset:* 权限组）
-- =====================================================================

ALTER TABLE `park_asset`
  MODIFY COLUMN `park_code` VARCHAR(64) DEFAULT NULL COMMENT '归属机构编码，NULL=平台素材';

-- idx_asset_url 索引已由基础 DDL 05_park.sql（757148a 折叠 park_asset 建表时含 KEY idx_asset_url）
-- 提供，本文件不再重复创建：否则 fresh init 在此报 ERROR 1061 中断编号链（README 铁律 2）。

INSERT INTO `system_dict_business`
  (`dict_type`, `dict_code`, `dict_name`, `dict_value`, `domain`, `sort_order`, `status`, `remark`,
   `created_at`, `updated_at`, `creator`, `updater`, `deleted`)
VALUES
  ('vr_provider', 'krpano',     'krpano',   'krpano',     'park', 1, 1, '全景漫游',       NOW(), NOW(), 'system', 'system', 0),
  ('vr_provider', 'threejs',    'Three.js', 'threejs',    'park', 2, 1, '3D 模型渲染',   NOW(), NOW(), 'system', 'system', 0),
  ('vr_provider', 'pano_image', '全景图',   'pano_image', 'park', 3, 1, '2:1 全景图直出', NOW(), NOW(), 'system', 'system', 0)
ON DUPLICATE KEY UPDATE `updated_at` = `updated_at`;

INSERT INTO `system_menu`
  (`menu_code`, `menu_name`, `parent_code`, `menu_type`, `path`, `component`, `permission_code`,
   `icon`, `sort_order`, `is_visible`, `domain_type`, `status`, `remark`,
   `created_at`, `updated_at`, `creator`, `updater`, `deleted`)
VALUES
  ('admin_resource_asset', '素材库', 'admin_resource', 2, '/resource/asset', 'resource/asset/index',
   'park:asset:list', 'PictureFilled', 6, 1, 'admin', 1, '全局素材库（含平台素材）',
   NOW(), NOW(), 'system', 'system', 0)
ON DUPLICATE KEY UPDATE `menu_code` = `menu_code`;
