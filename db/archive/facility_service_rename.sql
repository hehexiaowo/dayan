-- =====================================================================
-- facility_service_rename.sql
-- 统一 5 张 type 表命名风格：
--   park_facility      → park_facility_type（对齐 room_type/care_type/food_type）
--   park_service_item  → park_service_type（对齐同上）
-- 字段同步重命名：facility_code→facility_type_code, service_code→service_type_code 等。
-- 关联表（park_pricing.ref_type / park_pricing_item.item_type / park_asset.source_type）
--   的数据值同步更新：'facility'→'facility_type', 'service_item'→'service_type'。
-- =====================================================================

-- ---------------------------------------------------------------------
-- 1. RENAME TABLE（MySQL 自动带上索引和约束）
-- ---------------------------------------------------------------------
RENAME TABLE `park_facility` TO `park_facility_type`;
RENAME TABLE `park_service_item` TO `park_service_type`;

-- ---------------------------------------------------------------------
-- 2. park_facility_type 字段重命名
-- ---------------------------------------------------------------------
ALTER TABLE `park_facility_type`
  CHANGE `facility_code` `facility_type_code` VARCHAR(50) NOT NULL COMMENT '设施类型编码',
  CHANGE `facility_name` `facility_type_name` VARCHAR(100) NOT NULL COMMENT '设施类型名称（如"健身房"、"棋牌室"、"医疗室"、"阅览室"）',
  CHANGE `facility_category` `facility_type_category` TINYINT(2) NOT NULL DEFAULT 1 COMMENT '设施类别（1=休闲娱乐, 2=医疗健康, 3=运动健身, 4=文化教育, 5=生活服务, 6=安全保障）',
  CHANGE `facility_description` `facility_type_description` TEXT DEFAULT NULL COMMENT '设施详细描述';

-- 唯一索引：uk_facility_code 引用的列名已随 CHANGE 更新，索引名保持不变（MySQL 自动绑定新列名）。

-- ---------------------------------------------------------------------
-- 3. park_service_type 字段重命名
-- ---------------------------------------------------------------------
ALTER TABLE `park_service_type`
  CHANGE `service_code` `service_type_code` VARCHAR(50) NOT NULL COMMENT '服务类型编码',
  CHANGE `service_name` `service_type_name` VARCHAR(100) NOT NULL COMMENT '服务类型名称（如"24小时护理"、"康复训练"、"心理疏导"）',
  CHANGE `service_category` `service_type_category` TINYINT(2) NOT NULL DEFAULT 1 COMMENT '服务类别（1=生活照料, 2=医疗健康, 3=康复训练, 4=文化娱乐, 5=心理关怀, 6=其他）',
  CHANGE `service_description` `service_type_description` TEXT DEFAULT NULL COMMENT '服务详细描述',
  CHANGE `service_frequency` `service_type_frequency` VARCHAR(100) DEFAULT NULL COMMENT '服务频次（如"每日3次"、"按需"）',
  CHANGE `service_duration` `service_type_duration` VARCHAR(50) DEFAULT NULL COMMENT '服务时长（如"每次1小时"）';

-- ---------------------------------------------------------------------
-- 4. 同步 park_pricing.ref_type 数据值
-- ---------------------------------------------------------------------
UPDATE `park_pricing` SET `ref_type` = 'facility_type' WHERE `ref_type` = 'facility';
UPDATE `park_pricing` SET `ref_type` = 'service_type' WHERE `ref_type` = 'service_item';

-- ---------------------------------------------------------------------
-- 5. 同步 park_pricing_item.item_type 数据值
-- ---------------------------------------------------------------------
UPDATE `park_pricing_item` SET `item_type` = 'facility_type' WHERE `item_type` = 'facility';
UPDATE `park_pricing_item` SET `item_type` = 'service_type' WHERE `item_type` = 'service_item';

-- ---------------------------------------------------------------------
-- 6. 同步 park_asset.source_type 数据值
-- ---------------------------------------------------------------------
UPDATE `park_asset` SET `source_type` = 'facility_type' WHERE `source_type` = 'facility';
UPDATE `park_asset` SET `source_type` = 'service_type' WHERE `source_type` = 'service_item';

-- ---------------------------------------------------------------------
-- 7. 同步 RBAC 权限码 + API 路径（supplier_permission + supplier_role_permission_ship）
-- ---------------------------------------------------------------------
UPDATE `supplier_permission` SET
  `permission_code` = REPLACE(`permission_code`, 'park:facility:', 'park:facility-type:'),
  `parent_code` = 'park:facility-type',
  `path` = REPLACE(`path`, '/admin-api/park/facility', '/admin-api/park/facility-type')
WHERE `permission_code` LIKE 'park:facility:%';

UPDATE `supplier_permission` SET
  `permission_code` = REPLACE(`permission_code`, 'park:service-item:', 'park:service-type:'),
  `parent_code` = 'park:service-type',
  `path` = REPLACE(`path`, '/admin-api/park/service-item', '/admin-api/park/service-type')
WHERE `permission_code` LIKE 'park:service-item:%';

UPDATE `supplier_role_permission_ship` SET
  `permission_code` = REPLACE(`permission_code`, 'park:facility:', 'park:facility-type:')
WHERE `permission_code` LIKE 'park:facility:%';

UPDATE `supplier_role_permission_ship` SET
  `permission_code` = REPLACE(`permission_code`, 'park:service-item:', 'park:service-type:')
WHERE `permission_code` LIKE 'park:service-item:%';
