SET NAMES utf8mb4;
-- =====================================================================
-- 55_asset_ref_hierarchy.sql  素材分类字典两级关联化
--
-- 背景：素材分类三元组中「类型1 业务维度」原为前端硬编码常量，未入字典；
--       类型2 为平铺字典。本文件将两级都纳入字典并以 parent_code 关联：
--       asset_ref_type1（一级：业务维度）+ asset_ref_type2（二级：细分分类，
--       parent_code 指向 asset_ref_type1 的 dict_code；通用分类 parent 为空，
--       表示任意业务维度可用）。
-- 归属映射：
--   park   → room_type/food_type/facility_type/service_type/display_block/
--            adviser/park_info/data_migration（机构域专属来源）
--   goods  → goods；content → content；course → course；scene → scene
--   （通用）→ media_mgmt/appearance/room_view/dining/activity/promo_video/
--            qualification/contract/panorama/brand（跨维度视觉/文件分类）
-- 表结构零改动（system_dict.parent_code/level 既有）；前端类型2 选项按
-- 已选类型1 级联（父级匹配或父级为空）。
-- =====================================================================

-- 一级：业务维度（与 REF_TYPE1_OPTIONS 常量一致，前端字典驱动化后以字典为准）
INSERT INTO `system_dict`
  (`dict_type`, `dict_code`, `dict_name`, `dict_value`, `parent_code`, `level`, `domain`,
   `sort_order`, `icon`, `css_class`, `extra`, `status`, `is_default`, `remark`,
   `created_at`, `updated_at`, `creator`, `updater`, `deleted`)
VALUES
  ('asset_ref_type1', 'park',     '机构素材', 'park',     NULL, 1, 'system', 1, NULL, NULL, NULL, 1, 0, '素材一级分类：养老机构维度', NOW(), NOW(), 'system', 'system', 0),
  ('asset_ref_type1', 'platform', '平台素材', 'platform', NULL, 1, 'system', 2, NULL, NULL, NULL, 1, 0, '素材一级分类：平台自有素材', NOW(), NOW(), 'system', 'system', 0),
  ('asset_ref_type1', 'goods',    '商品素材', 'goods',    NULL, 1, 'system', 3, NULL, NULL, NULL, 1, 0, '素材一级分类：商品维度', NOW(), NOW(), 'system', 'system', 0),
  ('asset_ref_type1', 'content',  '内容素材', 'content',  NULL, 1, 'system', 4, NULL, NULL, NULL, 1, 0, '素材一级分类：内容维度', NOW(), NOW(), 'system', 'system', 0),
  ('asset_ref_type1', 'course',   '课程素材', 'course',   NULL, 1, 'system', 5, NULL, NULL, NULL, 1, 0, '素材一级分类：课程维度', NOW(), NOW(), 'system', 'system', 0),
  ('asset_ref_type1', 'scene',    '场景素材', 'scene',    NULL, 1, 'system', 6, NULL, NULL, NULL, 1, 0, '素材一级分类：场景维度', NOW(), NOW(), 'system', 'system', 0)
ON DUPLICATE KEY UPDATE `updated_at` = `updated_at`;

-- 二级：细分分类挂父级（park 域专属来源）
UPDATE `system_dict` SET `parent_code` = 'park', `level` = 2, `updated_at` = NOW()
 WHERE `dict_type` = 'asset_ref_type2' AND `dict_code` IN
   ('room_type', 'food_type', 'facility_type', 'service_type', 'display_block',
    'adviser', 'park_info', 'data_migration') AND `deleted` = 0;

-- 二级：各业务域自有分类
UPDATE `system_dict` SET `parent_code` = 'goods',  `level` = 2, `updated_at` = NOW()
 WHERE `dict_type` = 'asset_ref_type2' AND `dict_code` = 'goods'  AND `deleted` = 0;
UPDATE `system_dict` SET `parent_code` = 'content', `level` = 2, `updated_at` = NOW()
 WHERE `dict_type` = 'asset_ref_type2' AND `dict_code` = 'content' AND `deleted` = 0;
UPDATE `system_dict` SET `parent_code` = 'course',  `level` = 2, `updated_at` = NOW()
 WHERE `dict_type` = 'asset_ref_type2' AND `dict_code` = 'course'  AND `deleted` = 0;
UPDATE `system_dict` SET `parent_code` = 'scene',   `level` = 2, `updated_at` = NOW()
 WHERE `dict_type` = 'asset_ref_type2' AND `dict_code` = 'scene'   AND `deleted` = 0;

-- 二级：跨维度通用分类（parent 保持空，level=2）
UPDATE `system_dict` SET `parent_code` = NULL, `level` = 2, `updated_at` = NOW()
 WHERE `dict_type` = 'asset_ref_type2' AND `dict_code` IN
   ('media_mgmt', 'appearance', 'room_view', 'dining', 'activity', 'promo_video',
    'qualification', 'contract', 'panorama', 'brand') AND `deleted` = 0;
