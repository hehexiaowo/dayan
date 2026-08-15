SET NAMES utf8mb4;
-- =====================================================================
-- 53_asset_ref_trio.sql  素材仓库分类体系泛化（park 专属 → 通用三元组）
--
-- 模型：素材仓库 = 全系统文件/地址登记中心（不绑定机构）；
--       真实引用关系由各业务表持有（park_room_type.images / goods_info.cover_image 等），
--       删除保护由 AssetRefMap 反查业务表实现，本表只存地址 + 冗余分类。
-- 1. 加 ref_type1（类型1：业务维度 park/platform/goods/content/course/scene）
--    + ref_type2（类型2：细分分类，字典 asset_ref_type2）
--    + ref_code（关联编码：业务实体编码，平台素材为空）
-- 2. 存量回填：ref_type1=park_code 非空→'park' 否则 'platform'；
--    ref_type2=source_type（展示板块309/房型135/迁移229/餐饮13/直录）；
--    ref_code=park_code。asset_category 存量 689/691 为 NULL，弃用不回填；
--    source_ref_code（子实体编码）为冗余（真引用在业务表），弃用。
-- 3. 删旧列 park_code / asset_category / source_type / source_ref_code
--    （idx_park_type / idx_source 随列自动删除），新增 idx_ref。
-- 4. 字典 asset_ref_type2（domain=system）。
-- =====================================================================

ALTER TABLE `system_asset`
  ADD COLUMN `ref_type1` VARCHAR(64) DEFAULT NULL COMMENT '类型1：业务维度（park机构/platform平台/goods商品/content内容/course课程/scene场景）' AFTER `asset_type`,
  ADD COLUMN `ref_type2` VARCHAR(64) DEFAULT NULL COMMENT '类型2：细分分类（字典 asset_ref_type2：room_type房型/display_block展示板块/appearance外观…）' AFTER `ref_type1`,
  ADD COLUMN `ref_code` VARCHAR(64) DEFAULT NULL COMMENT '关联编码（业务实体编码，如机构编码/商品编码；平台素材为空）' AFTER `ref_type2`;

UPDATE `system_asset` SET
  `ref_type1` = CASE WHEN `park_code` IS NOT NULL AND `park_code` <> '' THEN 'park' ELSE 'platform' END,
  `ref_type2` = NULLIF(`source_type`, ''),
  `ref_code`  = NULLIF(`park_code`, '');

ALTER TABLE `system_asset`
  DROP COLUMN `park_code`,
  DROP COLUMN `asset_category`,
  DROP COLUMN `source_type`,
  DROP COLUMN `source_ref_code`,
  ADD KEY `idx_ref` (`ref_type1`, `ref_code`, `asset_type`);

INSERT INTO `system_dict_business`
  (`dict_type`, `dict_code`, `dict_name`, `dict_value`, `domain`, `sort_order`, `status`, `remark`,
   `created_at`, `updated_at`, `creator`, `updater`, `deleted`)
VALUES
  ('asset_ref_type2', 'media_mgmt',    '素材仓库直录', 'media_mgmt',    'system', 1, 1, '素材仓库直接上传/录入', NOW(), NOW(), 'system', 'system', 0),
  ('asset_ref_type2', 'room_type',     '房型',         'room_type',     'system', 2, 1, '机构-房型图', NOW(), NOW(), 'system', 'system', 0),
  ('asset_ref_type2', 'food_type',     '餐饮方案',     'food_type',     'system', 3, 1, '机构-餐饮图', NOW(), NOW(), 'system', 'system', 0),
  ('asset_ref_type2', 'facility_type', '设施',         'facility_type', 'system', 4, 1, '机构-设施图', NOW(), NOW(), 'system', 'system', 0),
  ('asset_ref_type2', 'service_type',  '服务项目',     'service_type',  'system', 5, 1, '机构-服务项目图', NOW(), NOW(), 'system', 'system', 0),
  ('asset_ref_type2', 'display_block', '展示板块',     'display_block', 'system', 6, 1, '机构-展示板块配图', NOW(), NOW(), 'system', 'system', 0),
  ('asset_ref_type2', 'adviser',       '顾问',         'adviser',       'system', 7, 1, '机构-顾问头像', NOW(), NOW(), 'system', 'system', 0),
  ('asset_ref_type2', 'park_info',     '机构信息',     'park_info',     'system', 8, 1, '机构-主信息配图', NOW(), NOW(), 'system', 'system', 0),
  ('asset_ref_type2', 'data_migration','存量迁移',     'data_migration','system', 9, 1, '老系统迁移素材', NOW(), NOW(), 'system', 'system', 0),
  ('asset_ref_type2', 'appearance',    '外观环境',     'appearance',    'system', 10, 1, '外观/大堂/花园等环境图', NOW(), NOW(), 'system', 'system', 0),
  ('asset_ref_type2', 'room_view',     '房间实景',     'room_view',     'system', 11, 1, '房间内部实景', NOW(), NOW(), 'system', 'system', 0),
  ('asset_ref_type2', 'dining',        '餐饮膳食',     'dining',        'system', 12, 1, '餐厅/膳食', NOW(), NOW(), 'system', 'system', 0),
  ('asset_ref_type2', 'activity',      '活动记录',     'activity',      'system', 13, 1, '文娱活动记录', NOW(), NOW(), 'system', 'system', 0),
  ('asset_ref_type2', 'promo_video',   '宣传视频',     'promo_video',   'system', 14, 1, '品牌/园区宣传', NOW(), NOW(), 'system', 'system', 0),
  ('asset_ref_type2', 'qualification', '资质文件',     'qualification', 'system', 15, 1, '资质/证照文件', NOW(), NOW(), 'system', 'system', 0),
  ('asset_ref_type2', 'contract',      '合同文件',     'contract',      'system', 16, 1, '合同/协议文件', NOW(), NOW(), 'system', 'system', 0),
  ('asset_ref_type2', 'panorama',      '全景VR',       'panorama',      'system', 17, 1, '全景漫游素材', NOW(), NOW(), 'system', 'system', 0),
  ('asset_ref_type2', 'goods',         '商品素材',     'goods',         'system', 18, 1, '商品主图/详情图/视频', NOW(), NOW(), 'system', 'system', 0),
  ('asset_ref_type2', 'content',       '内容素材',     'content',       'system', 19, 1, '内容封面/正文配图', NOW(), NOW(), 'system', 'system', 0),
  ('asset_ref_type2', 'course',        '课程素材',     'course',        'system', 20, 1, '课程封面/视频', NOW(), NOW(), 'system', 'system', 0),
  ('asset_ref_type2', 'scene',         '场景素材',     'scene',         'system', 21, 1, '场景封面/图集/视频', NOW(), NOW(), 'system', 'system', 0),
  ('asset_ref_type2', 'brand',         '品牌素材',     'brand',         'system', 22, 1, 'Logo/品牌介绍图', NOW(), NOW(), 'system', 'system', 0)
ON DUPLICATE KEY UPDATE `updated_at` = `updated_at`;
