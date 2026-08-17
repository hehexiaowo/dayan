-- 强制连接字符集为 utf8mb4，避免 docker-entrypoint-initdb.d 按默认字符集读取
-- 导致中文双重编码（Mojibake）。必须作为第一条语句执行。
SET NAMES utf8mb4;
-- =====================================================================
-- 12_goods.sql  商品域（goods_）
-- 域说明：商品基础信息 + 权益/场景/课程/旅居四类商品 SKU
-- 表数：5
-- 生成依据：docs/02数据库设计文档_v4.1.md §3.12
-- 主键策略：全部为平台共享表（AUTO_INCREMENT）
-- =====================================================================

-- ---------------------------------------------------------------------
-- 3.12.1 goods_info 商品信息（平台共享表，AUTO_INCREMENT）
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `goods_info`;
CREATE TABLE `goods_info` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `goods_code` VARCHAR(50) NOT NULL COMMENT '商品编码',
  `goods_name` VARCHAR(200) NOT NULL COMMENT '商品名称',
  `goods_short_name` VARCHAR(50) DEFAULT NULL COMMENT '商品简称',
  `goods_type` TINYINT(2) NOT NULL COMMENT '商品类型（1=养老权益, 2=场景营销, 3=培训课程, 4=旅游短居）',
  `category_code` VARCHAR(50) DEFAULT NULL COMMENT '分类编码',
  `brand_name` VARCHAR(100) DEFAULT NULL COMMENT '品牌名称',
  `cover_image` VARCHAR(500) DEFAULT NULL COMMENT '封面图URL',
  `image_urls` TEXT DEFAULT NULL COMMENT '商品图片（JSON数组）',
  `video_url` VARCHAR(500) DEFAULT NULL COMMENT '宣传视频URL',
  `goods_description` TEXT DEFAULT NULL COMMENT '商品详细描述',
  `summary` VARCHAR(500) DEFAULT NULL COMMENT '商品摘要',
  `original_price` DECIMAL(12,2) NOT NULL COMMENT '原价',
  `sale_price` DECIMAL(12,2) NOT NULL COMMENT '售价',
  `cost_price` DECIMAL(12,2) DEFAULT NULL COMMENT '成本价',
  `price_unit` VARCHAR(20) NOT NULL DEFAULT '元' COMMENT '价格单位',
  `stock` INT(11) NOT NULL DEFAULT -1 COMMENT '库存（-1=不限）',
  `sales_count` INT(11) NOT NULL DEFAULT 0 COMMENT '已售数量',
  `view_count` INT(11) NOT NULL DEFAULT 0 COMMENT '浏览次数',
  `collect_count` INT(11) NOT NULL DEFAULT 0 COMMENT '收藏次数',
  `sale_start_time` DATETIME DEFAULT NULL COMMENT '开售时间',
  `sale_end_time` DATETIME DEFAULT NULL COMMENT '停售时间',
  `is_hot` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否热销（0=否, 1=是）',
  `is_new` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否新品（0=否, 1=是）',
  `is_recommend` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否推荐（0=否, 1=是）',
  `sort_order` INT(11) NOT NULL DEFAULT 0 COMMENT '排序号',
  `goods_status` TINYINT(2) NOT NULL DEFAULT 0 COMMENT '商品状态（0=草稿, 1=待上架, 2=已上架, 3=已下架, 4=已售罄）',
  `audit_status` TINYINT(2) NOT NULL DEFAULT 0 COMMENT '审核状态（0=待审核, 1=审核通过, 2=审核驳回）',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `creator` VARCHAR(64) DEFAULT 'system' COMMENT '创建人',
  `updater` VARCHAR(64) DEFAULT 'system' COMMENT '更新人',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：1已删除/0未删除',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '删除时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_goods_code` (`goods_code`),
  KEY `idx_goods_type` (`goods_type`),
  KEY `idx_category_code` (`category_code`),
  KEY `idx_goods_status` (`goods_status`),
  KEY `idx_sort_order` (`sort_order`),
  KEY `idx_sale_price` (`sale_price`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商品信息';

-- ---------------------------------------------------------------------
-- 3.12.2 goods_sku_equity 权益商品SKU（平台共享表，AUTO_INCREMENT）
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `goods_sku_equity`;
CREATE TABLE `goods_sku_equity` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `goods_code` VARCHAR(50) NOT NULL COMMENT '商品编码',
  `sku_code` VARCHAR(50) NOT NULL COMMENT 'SKU编码',
  `sku_name` VARCHAR(200) NOT NULL COMMENT 'SKU名称',
  `template_code` VARCHAR(50) NOT NULL COMMENT '权益模板编码',
  `equity_type` TINYINT(2) NOT NULL COMMENT '权益类型',
  `equity_value` DECIMAL(12,2) NOT NULL COMMENT '权益面值',
  `sku_price` DECIMAL(12,2) NOT NULL COMMENT 'SKU售价',
  `stock` INT(11) NOT NULL DEFAULT -1 COMMENT '库存（-1=不限）',
  `sales_count` INT(11) NOT NULL DEFAULT 0 COMMENT '已售数量',
  `spec_description` VARCHAR(500) DEFAULT NULL COMMENT '规格描述',
  `sort_order` INT(11) NOT NULL DEFAULT 0 COMMENT '排序号',
  `status` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '状态（0=停售, 1=在售）',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `creator` VARCHAR(64) DEFAULT 'system' COMMENT '创建人',
  `updater` VARCHAR(64) DEFAULT 'system' COMMENT '更新人',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：1已删除/0未删除',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '删除时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sku_code` (`sku_code`),
  KEY `idx_goods_code` (`goods_code`),
  KEY `idx_template_code` (`template_code`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='权益商品SKU';

-- ---------------------------------------------------------------------
-- 3.12.3 goods_sku_scene 场景商品SKU（平台共享表，AUTO_INCREMENT）
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `goods_sku_scene`;
CREATE TABLE `goods_sku_scene` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `goods_code` VARCHAR(50) NOT NULL COMMENT '商品编码',
  `sku_code` VARCHAR(50) NOT NULL COMMENT 'SKU编码',
  `sku_name` VARCHAR(200) NOT NULL COMMENT 'SKU名称',
  `scene_code` VARCHAR(50) NOT NULL COMMENT '场景编码',
  `park_code` VARCHAR(64) NOT NULL COMMENT '关联机构编码',
  `sku_price` DECIMAL(12,2) NOT NULL COMMENT 'SKU售价',
  `person_limit` INT(11) NOT NULL DEFAULT 1 COMMENT '人数限制',
  `duration_hours` DECIMAL(4,1) DEFAULT NULL COMMENT '活动时长（小时）',
  `schedule_description` VARCHAR(500) DEFAULT NULL COMMENT '排期说明',
  `stock` INT(11) NOT NULL DEFAULT -1 COMMENT '库存',
  `sales_count` INT(11) NOT NULL DEFAULT 0 COMMENT '已售数量',
  `sort_order` INT(11) NOT NULL DEFAULT 0 COMMENT '排序号',
  `status` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '状态（0=停售, 1=在售）',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `creator` VARCHAR(64) DEFAULT 'system' COMMENT '创建人',
  `updater` VARCHAR(64) DEFAULT 'system' COMMENT '更新人',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：1已删除/0未删除',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '删除时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sku_code` (`sku_code`),
  KEY `idx_goods_code` (`goods_code`),
  KEY `idx_scene_code` (`scene_code`),
  KEY `idx_park_code` (`park_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='场景商品SKU';

-- ---------------------------------------------------------------------
-- 3.12.4 goods_sku_course 课程商品SKU（平台共享表，AUTO_INCREMENT）
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `goods_sku_course`;
CREATE TABLE `goods_sku_course` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `goods_code` VARCHAR(50) NOT NULL COMMENT '商品编码',
  `sku_code` VARCHAR(50) NOT NULL COMMENT 'SKU编码',
  `sku_name` VARCHAR(200) NOT NULL COMMENT 'SKU名称',
  `course_code` VARCHAR(50) NOT NULL COMMENT '课程编码',
  `course_type` TINYINT(2) NOT NULL COMMENT '课程类型（1=线上课程, 2=线下课程, 3=直播课程）',
  `sku_price` DECIMAL(12,2) NOT NULL COMMENT 'SKU售价',
  `class_count` INT(11) DEFAULT NULL COMMENT '课时数',
  `valid_days` INT(11) DEFAULT NULL COMMENT '有效天数',
  `stock` INT(11) NOT NULL DEFAULT -1 COMMENT '库存',
  `sales_count` INT(11) NOT NULL DEFAULT 0 COMMENT '已售数量',
  `sort_order` INT(11) NOT NULL DEFAULT 0 COMMENT '排序号',
  `status` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '状态（0=停售, 1=在售）',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `creator` VARCHAR(64) DEFAULT 'system' COMMENT '创建人',
  `updater` VARCHAR(64) DEFAULT 'system' COMMENT '更新人',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：1已删除/0未删除',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '删除时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sku_code` (`sku_code`),
  KEY `idx_goods_code` (`goods_code`),
  KEY `idx_course_code` (`course_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='课程商品SKU';

-- ---------------------------------------------------------------------
-- 3.12.5 goods_sku_sojourn 旅居房间商品SKU（平台共享表，AUTO_INCREMENT）
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `goods_sku_sojourn`;
CREATE TABLE `goods_sku_sojourn` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `goods_code` VARCHAR(50) NOT NULL COMMENT '商品编码',
  `sku_code` VARCHAR(50) NOT NULL COMMENT 'SKU编码',
  `sku_name` VARCHAR(200) NOT NULL COMMENT 'SKU名称',
  `park_code` VARCHAR(50) NOT NULL COMMENT '机构编码',
  `room_type_code` VARCHAR(64) NOT NULL COMMENT '关联房间类型编码',
  `room_type_name` VARCHAR(100) DEFAULT NULL COMMENT '房间类型名称',
  `care_type_code` VARCHAR(64) DEFAULT NULL COMMENT '关联照护类型编码',
  `food_type_code` VARCHAR(64) DEFAULT NULL COMMENT '关联餐饮类型编码',
  `sku_price` DECIMAL(12,2) NOT NULL COMMENT 'SKU售价',
  `price_unit` VARCHAR(20) NOT NULL DEFAULT '元/月' COMMENT '价格单位',
  `min_days` INT(11) NOT NULL DEFAULT 30 COMMENT '最少天数',
  `max_days` INT(11) DEFAULT NULL COMMENT '最多天数（NULL=不限）',
  `stock` INT(11) NOT NULL DEFAULT -1 COMMENT '库存',
  `sales_count` INT(11) NOT NULL DEFAULT 0 COMMENT '已售数量',
  `effective_date` DATE NOT NULL COMMENT '生效日期',
  `expire_date` DATE DEFAULT NULL COMMENT '失效日期',
  `sort_order` INT(11) NOT NULL DEFAULT 0 COMMENT '排序号',
  `status` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '状态（0=停售, 1=在售）',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `creator` VARCHAR(64) DEFAULT 'system' COMMENT '创建人',
  `updater` VARCHAR(64) DEFAULT 'system' COMMENT '更新人',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：1已删除/0未删除',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '删除时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sku_code` (`sku_code`),
  KEY `idx_goods_code` (`goods_code`),
  KEY `idx_park_code` (`park_code`),
  KEY `idx_room_type_code` (`room_type_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='旅居房间商品SKU';
