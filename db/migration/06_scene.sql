-- =====================================================================
-- 06_scene.sql  场景域（scene_）
-- 域说明：为保险公司/代理人策划的活动场景——主信息、项目明细、定价、日程、资源
-- 表数：5
-- 生成依据：docs/02数据库设计文档_v4.1.md §3.6
-- 主键策略：全部为平台共享表（AUTO_INCREMENT）
-- =====================================================================

-- ---------------------------------------------------------------------
-- 3.6.1 scene_info 场景信息
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `scene_info`;
CREATE TABLE `scene_info` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `scene_code` VARCHAR(50) NOT NULL COMMENT '场景编码',
  `scene_name` VARCHAR(200) NOT NULL COMMENT '场景名称',
  `scene_type` TINYINT(2) NOT NULL COMMENT '场景类型（1=参观体验, 2=健康讲座, 3=亲子互动, 4=节日活动, 5=文化娱乐, 6=健康检测, 7=美食品鉴, 8=其他）',
  `park_code` VARCHAR(50) NOT NULL COMMENT '关联养老机构编码',
  `province_code` VARCHAR(20) NOT NULL COMMENT '省份编码',
  `city_code` VARCHAR(20) NOT NULL COMMENT '城市编码',
  `district_code` VARCHAR(20) NOT NULL COMMENT '区划编码',
  `address` VARCHAR(500) DEFAULT NULL COMMENT '活动地址（可与机构地址不同）',
  `scene_description` TEXT DEFAULT NULL COMMENT '场景详细描述',
  `cover_image` VARCHAR(500) DEFAULT NULL COMMENT '封面图URL',
  `image_urls` TEXT DEFAULT NULL COMMENT '场景图片URL列表（JSON数组）',
  `video_url` VARCHAR(500) DEFAULT NULL COMMENT '宣传视频URL',
  `capacity` INT(11) NOT NULL DEFAULT 0 COMMENT '最大容纳人数',
  `duration_hours` DECIMAL(4,1) DEFAULT NULL COMMENT '预计时长（小时）',
  `target_audience` VARCHAR(500) DEFAULT NULL COMMENT '目标人群描述',
  `highlight` TEXT DEFAULT NULL COMMENT '场景亮点（JSON数组）',
  `notice` TEXT DEFAULT NULL COMMENT '注意事项',
  `min_person` INT(11) NOT NULL DEFAULT 1 COMMENT '最低成团人数',
  `max_person` INT(11) NOT NULL DEFAULT 50 COMMENT '最大参与人数',
  `original_price` DECIMAL(12,2) DEFAULT NULL COMMENT '原价',
  `sale_price` DECIMAL(12,2) DEFAULT NULL COMMENT '售价（0=免费）',
  `price_unit` VARCHAR(20) NOT NULL DEFAULT '元/人' COMMENT '价格单位',
  `is_free` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否免费（0=否, 1=是）',
  `sort_order` INT(11) NOT NULL DEFAULT 0 COMMENT '排序号',
  `view_count` INT(11) NOT NULL DEFAULT 0 COMMENT '浏览次数',
  `book_count` INT(11) NOT NULL DEFAULT 0 COMMENT '预约次数',
  `scene_status` TINYINT(2) NOT NULL DEFAULT 0 COMMENT '场景状态（0=草稿, 1=已上架, 2=已下架, 3=已满期）',
  `audit_status` TINYINT(2) NOT NULL DEFAULT 0 COMMENT '审核状态（0=待审核, 1=审核通过, 2=审核驳回）',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `creator` VARCHAR(64) DEFAULT 'system' COMMENT '创建人',
  `updater` VARCHAR(64) DEFAULT 'system' COMMENT '更新人',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：1已删除/0未删除',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '删除时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_scene_code` (`scene_code`),
  KEY `idx_scene_type` (`scene_type`),
  KEY `idx_park_code` (`park_code`),
  KEY `idx_city_code` (`city_code`),
  KEY `idx_scene_status` (`scene_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='场景信息';

-- ---------------------------------------------------------------------
-- 3.6.2 scene_item 场景项目明细
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `scene_item`;
CREATE TABLE `scene_item` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `scene_code` VARCHAR(64) NOT NULL COMMENT '场景编码',
  `item_code` VARCHAR(50) NOT NULL COMMENT '项目编码',
  `item_name` VARCHAR(100) NOT NULL COMMENT '项目名称',
  `item_type` TINYINT(2) NOT NULL DEFAULT 1 COMMENT '项目类型（1=体验项目, 2=讲座环节, 3=互动游戏, 4=餐饮服务, 5=检测项目, 6=赠品）',
  `item_description` VARCHAR(500) DEFAULT NULL COMMENT '项目描述',
  `duration_minutes` INT(11) DEFAULT NULL COMMENT '预计时长（分钟）',
  `sort_order` INT(11) NOT NULL DEFAULT 0 COMMENT '排序号（代表环节顺序）',
  `is_required` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否必选参与（0=否, 1=是）',
  `status` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '状态（0=禁用, 1=启用）',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `creator` VARCHAR(64) DEFAULT 'system' COMMENT '创建人',
  `updater` VARCHAR(64) DEFAULT 'system' COMMENT '更新人',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：1已删除/0未删除',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '删除时间',
  PRIMARY KEY (`id`),
  KEY `idx_scene_code` (`scene_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='场景项目明细';

-- ---------------------------------------------------------------------
-- 3.6.3 scene_item_price 场景项目定价
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `scene_item_price`;
CREATE TABLE `scene_item_price` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `scene_code` VARCHAR(64) NOT NULL COMMENT '场景编码',
  `scene_item_code` VARCHAR(64) DEFAULT NULL COMMENT '场景项目编码（NULL=整体定价）',
  `price_type` TINYINT(2) NOT NULL DEFAULT 1 COMMENT '定价类型（1=按人, 2=按组, 3=按场）',
  `original_price` DECIMAL(12,2) NOT NULL COMMENT '原价',
  `sale_price` DECIMAL(12,2) NOT NULL COMMENT '售价',
  `channel_price` DECIMAL(12,2) DEFAULT NULL COMMENT '渠道专属价',
  `price_description` VARCHAR(200) DEFAULT NULL COMMENT '价格说明',
  `effective_date` DATE NOT NULL COMMENT '生效日期',
  `expire_date` DATE DEFAULT NULL COMMENT '失效日期',
  `status` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '状态（0=禁用, 1=启用）',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `creator` VARCHAR(64) DEFAULT 'system' COMMENT '创建人',
  `updater` VARCHAR(64) DEFAULT 'system' COMMENT '更新人',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：1已删除/0未删除',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '删除时间',
  PRIMARY KEY (`id`),
  KEY `idx_scene_code` (`scene_code`),
  KEY `idx_scene_item_code` (`scene_item_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='场景项目定价';

-- ---------------------------------------------------------------------
-- 3.6.4 scene_schedule 场景日程安排
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `scene_schedule`;
CREATE TABLE `scene_schedule` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `scene_code` VARCHAR(64) NOT NULL COMMENT '场景编码',
  `schedule_date` DATE NOT NULL COMMENT '活动日期',
  `start_time` TIME NOT NULL COMMENT '开始时间',
  `end_time` TIME NOT NULL COMMENT '结束时间',
  `max_person` INT(11) NOT NULL DEFAULT 0 COMMENT '最大参与人数',
  `current_person` INT(11) NOT NULL DEFAULT 0 COMMENT '已报名人数',
  `price_override` DECIMAL(12,2) DEFAULT NULL COMMENT '当日特殊价格（NULL=使用默认价）',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `status` TINYINT(2) NOT NULL DEFAULT 1 COMMENT '状态（0=已取消, 1=可预约, 2=已约满, 3=进行中, 4=已结束）',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `creator` VARCHAR(64) DEFAULT 'system' COMMENT '创建人',
  `updater` VARCHAR(64) DEFAULT 'system' COMMENT '更新人',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：1已删除/0未删除',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '删除时间',
  PRIMARY KEY (`id`),
  KEY `idx_scene_code` (`scene_code`),
  KEY `idx_schedule_date` (`schedule_date`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='场景日程安排';

-- ---------------------------------------------------------------------
-- 3.6.5 scene_resource 场景所需资源
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `scene_resource`;
CREATE TABLE `scene_resource` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `scene_code` VARCHAR(64) NOT NULL COMMENT '场景编码',
  `resource_type` TINYINT(2) NOT NULL DEFAULT 1 COMMENT '资源类型（1=场地, 2=设备, 3=物料, 4=人员, 5=餐饮）',
  `resource_name` VARCHAR(100) NOT NULL COMMENT '资源名称',
  `resource_description` VARCHAR(500) DEFAULT NULL COMMENT '资源描述',
  `quantity` INT(11) NOT NULL DEFAULT 1 COMMENT '数量',
  `unit` VARCHAR(20) DEFAULT NULL COMMENT '单位',
  `unit_cost` DECIMAL(12,2) DEFAULT NULL COMMENT '单位成本',
  `is_provided` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否由机构提供（0=否需自备, 1=是机构提供）',
  `sort_order` INT(11) NOT NULL DEFAULT 0 COMMENT '排序号',
  `status` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '状态（0=禁用, 1=启用）',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `creator` VARCHAR(64) DEFAULT 'system' COMMENT '创建人',
  `updater` VARCHAR(64) DEFAULT 'system' COMMENT '更新人',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：1已删除/0未删除',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '删除时间',
  PRIMARY KEY (`id`),
  KEY `idx_scene_code` (`scene_code`),
  KEY `idx_resource_type` (`resource_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='场景所需资源';
