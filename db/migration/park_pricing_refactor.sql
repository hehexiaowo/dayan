-- =====================================================================
-- park_pricing_refactor.sql
-- 养老机构价格表合并重构 + 评分拆表 + P0 修复
--
-- 改动清单：
--   1. 新建 park_pricing（合并 5 张 price 表）
--   2. 新建 park_pricing_item（定价明细行）
--   3. 新建 park_score（评分独立表）
--   4. 迁移旧 price 数据 → park_pricing
--   5. 迁移 score 数据 → park_score
--   6. ALTER park_info：删除 score_* 列、经纬度改 DECIMAL
--   7. DROP 5 张旧 price 表
-- =====================================================================

-- ---------------------------------------------------------------------
-- 1. 新建 park_pricing
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `park_pricing`;
CREATE TABLE `park_pricing` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `park_code` VARCHAR(64) NOT NULL COMMENT '机构编码',
  `plan_name` VARCHAR(100) DEFAULT NULL COMMENT '方案名称（如"豪华单人间·月费"）',
  `charge_type` TINYINT NOT NULL COMMENT '费类（1=房间费 2=照护费 3=餐费 4=押金 5=设施费 6=服务费 9=其他）',
  `ref_type` VARCHAR(20) NOT NULL COMMENT '关联类型（room_type/care_type/food_type/facility/service_item/park）',
  `ref_code` VARCHAR(64) NOT NULL COMMENT '关联编码',
  `ref_name` VARCHAR(100) DEFAULT NULL COMMENT '关联名称（冗余）',
  `billing_cycle` TINYINT DEFAULT NULL COMMENT '计费周期（1=月 2=季 3=半年 4=年 5=一次性）',
  `price_unit` VARCHAR(50) DEFAULT NULL COMMENT '自由文本计费单位（设施/服务的 次/小时/场）',
  `original_price` DECIMAL(12,2) DEFAULT NULL COMMENT '原价',
  `sale_price` DECIMAL(12,2) NOT NULL COMMENT '售价',
  `discount_rate` DECIMAL(5,2) DEFAULT NULL COMMENT '折扣率',
  `price_description` VARCHAR(500) DEFAULT NULL COMMENT '价格说明',
  `includes_items` TEXT DEFAULT NULL COMMENT '包含项目（JSON数组）',
  `effective_date` DATE NOT NULL COMMENT '生效日期',
  `expire_date` DATE DEFAULT NULL COMMENT '失效日期（NULL=长期有效）',
  `is_current` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否当前生效价格（0=历史 1=当前）',
  `is_promotion` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否促销价',
  `promotion_description` VARCHAR(200) DEFAULT NULL COMMENT '促销说明',
  `price_change_reason` VARCHAR(500) DEFAULT NULL COMMENT '价格变更原因',
  `sort_order` INT(11) NOT NULL DEFAULT 0 COMMENT '排序号',
  `status` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '状态（0=停用 1=启用）',
  `version` BIGINT(20) NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `creator` VARCHAR(64) DEFAULT 'system' COMMENT '创建人',
  `updater` VARCHAR(64) DEFAULT 'system' COMMENT '更新人',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：1已删除/0未删除',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '删除时间',
  -- 条件唯一索引：同 park+charge_type+ref_code+billing_cycle 下 is_current=1 唯一
  `current_key` VARCHAR(200) GENERATED ALWAYS AS
    (CASE WHEN `is_current` = 1 AND `deleted` = 0
     THEN CONCAT(`park_code`, '|', `charge_type`, '|', `ref_code`, '|', COALESCE(CAST(`billing_cycle` AS CHAR), '0'))
     ELSE NULL END) STORED COMMENT '当前价唯一键（生成列）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_current` (`current_key`),
  KEY `idx_park_code` (`park_code`),
  KEY `idx_charge_type` (`charge_type`),
  KEY `idx_ref` (`ref_type`, `ref_code`),
  KEY `idx_is_current` (`is_current`),
  KEY `idx_effective_date` (`effective_date`),
  KEY `idx_park_charge` (`park_code`, `charge_type`, `billing_cycle`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='机构统一定价方案';

-- ---------------------------------------------------------------------
-- 2. 新建 park_pricing_item
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `park_pricing_item`;
CREATE TABLE `park_pricing_item` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `pricing_id` BIGINT NOT NULL COMMENT 'FK→park_pricing.id',
  `park_code` VARCHAR(64) NOT NULL COMMENT '机构编码',
  `item_type` VARCHAR(20) NOT NULL COMMENT '关联类型（room_type/care_type/food_type/facility/service_item）',
  `item_code` VARCHAR(64) NOT NULL COMMENT '关联编码',
  `item_name` VARCHAR(100) DEFAULT NULL COMMENT '关联名称（冗余）',
  `sort_order` INT(11) NOT NULL DEFAULT 0 COMMENT '排序号',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `creator` VARCHAR(64) DEFAULT 'system' COMMENT '创建人',
  `updater` VARCHAR(64) DEFAULT 'system' COMMENT '更新人',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：1已删除/0未删除',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '删除时间',
  PRIMARY KEY (`id`),
  KEY `idx_pricing_id` (`pricing_id`),
  KEY `idx_item` (`park_code`, `item_type`, `item_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='机构定价明细行（套餐关联）';

-- ---------------------------------------------------------------------
-- 3. 新建 park_score
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `park_score`;
CREATE TABLE `park_score` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `park_code` VARCHAR(64) NOT NULL COMMENT '机构编码',
  `score_total` INT(11) DEFAULT NULL COMMENT '总评分',
  `score_environment` INT(11) DEFAULT NULL COMMENT '环境评分',
  `score_recreation` INT(11) DEFAULT NULL COMMENT '文娱评分',
  `score_nursing` INT(11) DEFAULT NULL COMMENT '医养护理评分',
  `score_food` INT(11) DEFAULT NULL COMMENT '餐食精细评分',
  `score_service` INT(11) DEFAULT NULL COMMENT '服务品质评分',
  `score_price` INT(11) DEFAULT NULL COMMENT '价格评分',
  `score_description` VARCHAR(255) DEFAULT NULL COMMENT '评分描述',
  `version` BIGINT(20) NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `creator` VARCHAR(64) DEFAULT 'system' COMMENT '创建人',
  `updater` VARCHAR(64) DEFAULT 'system' COMMENT '更新人',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：1已删除/0未删除',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '删除时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_park_code` (`park_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='机构评分（独立表）';

-- ---------------------------------------------------------------------
-- 4. 迁移旧 price 数据 → park_pricing
-- ---------------------------------------------------------------------

-- 4.1 park_room_price → charge_type=1
INSERT INTO `park_pricing` (
  `park_code`, `charge_type`, `ref_type`, `ref_code`, `billing_cycle`,
  `original_price`, `sale_price`, `discount_rate`, `price_description`,
  `includes_items`, `effective_date`, `expire_date`, `is_current`,
  `is_promotion`, `promotion_description`, `price_change_reason`,
  `sort_order`, `status`, `created_at`, `updated_at`, `creator`, `updater`, `deleted`
)
SELECT
  `park_code`, 1, 'room_type', `room_type_code`, `price_type`,
  `original_price`, `sale_price`, `discount_rate`, `price_description`,
  `includes_items`, `effective_date`, `expire_date`, `is_current`,
  `is_promotion`, `promotion_description`, `price_change_reason`,
  `sort_order`, `status`, `created_at`, `updated_at`, `creator`, `updater`, `deleted`
FROM `park_room_price`;

-- 4.2 park_care_price → charge_type=2
INSERT INTO `park_pricing` (
  `park_code`, `charge_type`, `ref_type`, `ref_code`, `billing_cycle`,
  `original_price`, `sale_price`, `discount_rate`, `price_description`,
  `effective_date`, `expire_date`, `is_current`,
  `is_promotion`, `promotion_description`,
  `sort_order`, `status`, `created_at`, `updated_at`, `creator`, `updater`, `deleted`
)
SELECT
  `park_code`, 2, 'care_type', `care_type_code`, `price_type`,
  `original_price`, `sale_price`, `discount_rate`, `price_description`,
  `effective_date`, `expire_date`, `is_current`,
  `is_promotion`, `promotion_description`,
  `sort_order`, `status`, `created_at`, `updated_at`, `creator`, `updater`, `deleted`
FROM `park_care_price`;

-- 4.3 park_food_price → charge_type=3
INSERT INTO `park_pricing` (
  `park_code`, `charge_type`, `ref_type`, `ref_code`, `billing_cycle`,
  `original_price`, `sale_price`, `discount_rate`, `price_description`,
  `effective_date`, `expire_date`, `is_current`,
  `is_promotion`, `promotion_description`,
  `sort_order`, `status`, `created_at`, `updated_at`, `creator`, `updater`, `deleted`
)
SELECT
  `park_code`, 3, 'food_type', `food_type_code`, `price_type`,
  `original_price`, `sale_price`, `discount_rate`, `price_description`,
  `effective_date`, `expire_date`, `is_current`,
  `is_promotion`, `promotion_description`,
  `sort_order`, `status`, `created_at`, `updated_at`, `creator`, `updater`, `deleted`
FROM `park_food_price`;

-- 4.4 park_facility_price → charge_type=5
INSERT INTO `park_pricing` (
  `park_code`, `charge_type`, `ref_type`, `ref_code`, `price_unit`,
  `original_price`, `sale_price`, `discount_rate`, `price_description`,
  `effective_date`, `expire_date`, `is_current`,
  `is_promotion`, `promotion_description`,
  `sort_order`, `status`, `created_at`, `updated_at`, `creator`, `updater`, `deleted`
)
SELECT
  `park_code`, 5, 'facility', `facility_code`, `price_unit`,
  `original_price`, `sale_price`, `discount_rate`, `price_description`,
  `effective_date`, `expire_date`, `is_current`,
  `is_promotion`, `promotion_description`,
  `sort_order`, `status`, `created_at`, `updated_at`, `creator`, `updater`, `deleted`
FROM `park_facility_price`;

-- 4.5 park_service_price → charge_type=6
INSERT INTO `park_pricing` (
  `park_code`, `charge_type`, `ref_type`, `ref_code`, `price_unit`,
  `original_price`, `sale_price`, `discount_rate`, `price_description`,
  `effective_date`, `expire_date`, `is_current`,
  `is_promotion`, `promotion_description`,
  `sort_order`, `status`, `created_at`, `updated_at`, `creator`, `updater`, `deleted`
)
SELECT
  `park_code`, 6, 'service_item', `service_code`, `price_unit`,
  `original_price`, `sale_price`, `discount_rate`, `price_description`,
  `effective_date`, `expire_date`, `is_current`,
  `is_promotion`, `promotion_description`,
  `sort_order`, `status`, `created_at`, `updated_at`, `creator`, `updater`, `deleted`
FROM `park_service_price`;

-- ---------------------------------------------------------------------
-- 5. 生成 park_pricing_item（每条 pricing 对应一条 item 主行）
-- ---------------------------------------------------------------------
INSERT INTO `park_pricing_item` (
  `pricing_id`, `park_code`, `item_type`, `item_code`, `sort_order`, `created_at`, `updated_at`, `creator`, `updater`, `deleted`
)
SELECT
  `id`, `park_code`, `ref_type`, `ref_code`, 0, `created_at`, `updated_at`, `creator`, `updater`, `deleted`
FROM `park_pricing`;

-- ---------------------------------------------------------------------
-- 6. 迁移 score 数据 → park_score
-- ---------------------------------------------------------------------
INSERT INTO `park_score` (
  `park_code`, `score_total`, `score_environment`, `score_recreation`,
  `score_nursing`, `score_food`, `score_service`, `score_price`,
  `score_description`, `created_at`, `updated_at`, `creator`, `updater`, `deleted`
)
SELECT
  `park_code`, `score_total`, `score_environment`, `score_recreation`,
  `score_nursing`, `score_food`, `score_service`, `score_price`,
  `score_description`, `created_at`, `updated_at`, `creator`, `updater`, `deleted`
FROM `park_info`
WHERE `deleted` = 0;

-- ---------------------------------------------------------------------
-- 7. ALTER park_info：删除 score_* 列 + 经纬度改 DECIMAL
-- ---------------------------------------------------------------------
ALTER TABLE `park_info`
  DROP COLUMN `score_total`,
  DROP COLUMN `score_environment`,
  DROP COLUMN `score_recreation`,
  DROP COLUMN `score_nursing`,
  DROP COLUMN `score_food`,
  DROP COLUMN `score_service`,
  DROP COLUMN `score_price`,
  DROP COLUMN `score_description`;

ALTER TABLE `park_info`
  MODIFY `longitude` DECIMAL(10,6) DEFAULT NULL COMMENT '经度',
  MODIFY `latitude`  DECIMAL(10,6) DEFAULT NULL COMMENT '纬度';

-- ---------------------------------------------------------------------
-- 8. DROP 5 张旧 price 表
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `park_room_price`;
DROP TABLE IF EXISTS `park_care_price`;
DROP TABLE IF EXISTS `park_food_price`;
DROP TABLE IF EXISTS `park_facility_price`;
DROP TABLE IF EXISTS `park_service_price`;
