-- =====================================================================
-- park_asset_merge.sql  机构素材库统一表
--
-- 将 park_media_image / park_media_video / park_media_file / park_media_vr
-- 四张表合并为 park_asset 统一素材库注册表。
--
-- 核心定位：不管从哪个入口上传（素材库 tab / 房型 tab / 设施 tab / 展示板块 / 顾问 …），
-- 都通过 source_type + source_ref_code 追踪来源，形成统一管理视图。
--
-- 迁移步骤：
--   1. 建 park_asset 表
--   2. 从 4 张旧表迁移数据（source_type='media_mgmt'）
--   3. DROP 4 张旧表
-- =====================================================================

-- ---------------------------------------------------------------------
-- park_asset 机构素材库（统一管理所有来源的图片/视频/文件/VR）
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `park_asset`;
CREATE TABLE `park_asset` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `park_code` VARCHAR(64) NOT NULL COMMENT '机构编码',
  `asset_type` TINYINT NOT NULL COMMENT '素材类型（1=图片 2=视频 3=文件 4=VR）',
  `asset_url` VARCHAR(500) NOT NULL COMMENT '文件 OSS key（存 key 非完整 URL）',
  `asset_name` VARCHAR(200) DEFAULT NULL COMMENT '文件名称',
  `asset_category` TINYINT DEFAULT NULL COMMENT '业务分类（图片:1=外观..11=其他 视频:1=宣传..3=活动 文件:1=资质..5=其他 VR:1=全景..3=视频VR）',
  `description` VARCHAR(500) DEFAULT NULL COMMENT '描述',
  `file_size` BIGINT DEFAULT NULL COMMENT '文件大小（字节）',
  -- 类型专属列（nullable，按 asset_type 区分）
  `width` INT DEFAULT NULL COMMENT '图片宽度px（图片专属）',
  `height` INT DEFAULT NULL COMMENT '图片高度px（图片专属）',
  `is_cover` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否封面（图片专属 0=否 1=是）',
  `cover_url` VARCHAR(500) DEFAULT NULL COMMENT '封面图key（视频专属）',
  `duration` INT DEFAULT NULL COMMENT '时长秒（视频专属）',
  `file_format` VARCHAR(20) DEFAULT NULL COMMENT '文件格式（文件专属 pdf/doc/xls等）',
  `vr_provider` VARCHAR(100) DEFAULT NULL COMMENT 'VR服务提供商（VR专属）',
  `thumbnail_url` VARCHAR(500) DEFAULT NULL COMMENT '缩略图key（VR专属）',
  -- 来源追踪（核心设计）
  `source_type` VARCHAR(30) NOT NULL DEFAULT 'media_mgmt' COMMENT '来源（media_mgmt=素材库直传 room_type=房型 food_type=餐饮 facility_type=设施 service_type=服务 display_block=展示板块 adviser=顾问 park_info=机构信息）',
  `source_ref_code` VARCHAR(64) DEFAULT NULL COMMENT '来源编码（media_mgmt 时为 NULL）',
  `sort_order` INT(11) NOT NULL DEFAULT 0 COMMENT '排序号',
  `status` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '状态（0=隐藏 1=显示）',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `creator` VARCHAR(64) DEFAULT 'system' COMMENT '创建人',
  `updater` VARCHAR(64) DEFAULT 'system' COMMENT '更新人',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：1已删除/0未删除',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '删除时间',
  PRIMARY KEY (`id`),
  KEY `idx_park_type` (`park_code`, `asset_type`, `sort_order`),
  KEY `idx_source` (`source_type`, `source_ref_code`),
  KEY `idx_asset_url` (`asset_url`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='机构素材库（统一管理所有来源的图片/视频/文件/VR）';

-- ---------------------------------------------------------------------
-- 数据迁移：4 张旧表 → park_asset（source_type='media_mgmt'）
-- ---------------------------------------------------------------------

-- park_media_image → asset_type=1
INSERT INTO `park_asset` (
  `park_code`, `asset_type`, `asset_url`, `asset_name`, `asset_category`, `description`,
  `file_size`, `width`, `height`, `is_cover`,
  `source_type`, `source_ref_code`, `sort_order`, `status`,
  `created_at`, `updated_at`, `creator`, `updater`, `deleted`, `deleted_at`
)
SELECT
  `park_code`, 1, `image_url`, `image_name`, `image_type`, `image_description`,
  `file_size`, `width`, `height`, `is_cover`,
  'media_mgmt', NULL, `sort_order`, `status`,
  `created_at`, `updated_at`, `creator`, `updater`, `deleted`, `deleted_at`
FROM `park_media_image`;

-- park_media_video → asset_type=2
INSERT INTO `park_asset` (
  `park_code`, `asset_type`, `asset_url`, `asset_name`, `asset_category`, `description`,
  `file_size`, `cover_url`, `duration`,
  `source_type`, `source_ref_code`, `sort_order`, `status`,
  `created_at`, `updated_at`, `creator`, `updater`, `deleted`, `deleted_at`
)
SELECT
  `park_code`, 2, `video_url`, `video_name`, `video_type`, `video_description`,
  `file_size`, `cover_url`, `duration`,
  'media_mgmt', NULL, `sort_order`, `status`,
  `created_at`, `updated_at`, `creator`, `updater`, `deleted`, `deleted_at`
FROM `park_media_video`;

-- park_media_file → asset_type=3
INSERT INTO `park_asset` (
  `park_code`, `asset_type`, `asset_url`, `asset_name`, `asset_category`, `description`,
  `file_size`, `file_format`,
  `source_type`, `source_ref_code`, `sort_order`, `status`,
  `created_at`, `updated_at`, `creator`, `updater`, `deleted`, `deleted_at`
)
SELECT
  `park_code`, 3, `file_url`, `file_name`, `file_type`, `file_description`,
  `file_size`, `file_format`,
  'media_mgmt', NULL, `sort_order`, `status`,
  `created_at`, `updated_at`, `creator`, `updater`, `deleted`, `deleted_at`
FROM `park_media_file`;

-- park_media_vr → asset_type=4
INSERT INTO `park_asset` (
  `park_code`, `asset_type`, `asset_url`, `asset_name`, `asset_category`, `description`,
  `vr_provider`, `thumbnail_url`,
  `source_type`, `source_ref_code`, `sort_order`, `status`,
  `created_at`, `updated_at`, `creator`, `updater`, `deleted`, `deleted_at`
)
SELECT
  `park_code`, 4, `vr_url`, `vr_name`, `vr_type`, `vr_description`,
  `vr_provider`, `thumbnail_url`,
  'media_mgmt', NULL, `sort_order`, `status`,
  `created_at`, `updated_at`, `creator`, `updater`, `deleted`, `deleted_at`
FROM `park_media_vr`;

-- ---------------------------------------------------------------------
-- DROP 旧表（迁移完成确认后执行）
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `park_media_image`;
DROP TABLE IF EXISTS `park_media_video`;
DROP TABLE IF EXISTS `park_media_file`;
DROP TABLE IF EXISTS `park_media_vr`;
