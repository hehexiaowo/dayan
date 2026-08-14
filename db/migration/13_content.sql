-- 强制连接字符集为 utf8mb4，避免 docker-entrypoint-initdb.d 按默认字符集读取
-- 导致中文双重编码（Mojibake）。必须作为第一条语句执行。
SET NAMES utf8mb4;
-- =====================================================================
-- 13_content.sql  内容域（content_）
-- 域说明：内容信息、分类、多媒体资源、分享记录、阅读记录（图文/视频内容资源管理）
-- 表数：5
-- 生成依据：docs/02数据库设计文档_v4.1.md §3.13
-- 主键策略：content_info、content_category、content_media 为平台共享表（AUTO_INCREMENT）；
--           content_record_share、content_record_read 为分片表（雪花ID，应用层 SnowflakeId）
-- =====================================================================

-- ---------------------------------------------------------------------
-- 3.13.1 content_info 内容信息（平台共享表，AUTO_INCREMENT）
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `content_info`;
CREATE TABLE `content_info` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `content_code` VARCHAR(50) NOT NULL COMMENT '内容编码',
  `title` VARCHAR(200) NOT NULL COMMENT '标题',
  `subtitle` VARCHAR(200) DEFAULT NULL COMMENT '副标题',
  `content_type` TINYINT(2) NOT NULL COMMENT '内容类型（1=文章, 2=视频, 3=图片集, 4=专题, 5=问答）',
  `category_code` VARCHAR(50) DEFAULT NULL COMMENT '分类编码',
  `author_name` VARCHAR(50) DEFAULT NULL COMMENT '作者姓名',
  `author_avatar` VARCHAR(500) DEFAULT NULL COMMENT '作者头像',
  `cover_image` VARCHAR(500) DEFAULT NULL COMMENT '封面图URL',
  `summary` VARCHAR(500) DEFAULT NULL COMMENT '摘要',
  `content_body` LONGTEXT DEFAULT NULL COMMENT '正文内容（富文本HTML）',
  `source_type` TINYINT(2) NOT NULL DEFAULT 1 COMMENT '来源类型（1=原创, 2=转载, 3=采编）',
  `source_url` VARCHAR(500) DEFAULT NULL COMMENT '来源链接',
  `tags` VARCHAR(500) DEFAULT NULL COMMENT '标签（JSON数组）',
  `is_top` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否置顶（0=否, 1=是）',
  `is_recommend` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否推荐（0=否, 1=是）',
  `is_comment` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否允许评论（0=否, 1=是）',
  `view_count` INT(11) NOT NULL DEFAULT 0 COMMENT '浏览次数',
  `like_count` INT(11) NOT NULL DEFAULT 0 COMMENT '点赞次数',
  `comment_count` INT(11) NOT NULL DEFAULT 0 COMMENT '评论次数',
  `share_count` INT(11) NOT NULL DEFAULT 0 COMMENT '分享次数',
  `collect_count` INT(11) NOT NULL DEFAULT 0 COMMENT '收藏次数',
  `publish_time` DATETIME DEFAULT NULL COMMENT '发布时间',
  `sort_order` INT(11) NOT NULL DEFAULT 0 COMMENT '排序号',
  `content_status` TINYINT(2) NOT NULL DEFAULT 0 COMMENT '状态（0=草稿, 1=待审核, 2=已发布, 3=已下架, 4=已删除）',
  `audit_status` TINYINT(2) NOT NULL DEFAULT 0 COMMENT '审核状态（0=待审核, 1=审核通过, 2=审核驳回）',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `creator` VARCHAR(64) DEFAULT 'system' COMMENT '创建人',
  `updater` VARCHAR(64) DEFAULT 'system' COMMENT '更新人',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：1已删除/0未删除',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '删除时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_content_code` (`content_code`),
  KEY `idx_content_type` (`content_type`),
  KEY `idx_category_code` (`category_code`),
  KEY `idx_publish_time` (`publish_time`),
  KEY `idx_content_status` (`content_status`),
  KEY `idx_is_top` (`is_top`),
  KEY `idx_sort_order` (`sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='内容信息';

-- ---------------------------------------------------------------------
-- 3.13.2 content_category 内容分类（平台共享表，AUTO_INCREMENT）
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `content_category`;
CREATE TABLE `content_category` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `category_code` VARCHAR(50) NOT NULL COMMENT '分类编码',
  `category_name` VARCHAR(100) NOT NULL COMMENT '分类名称',
  `parent_code` VARCHAR(50) DEFAULT NULL COMMENT '父分类编码（NULL=顶级）',
  `category_type` TINYINT(2) NOT NULL DEFAULT 1 COMMENT '分类类型（1=文章分类, 2=视频分类, 3=专题分类）',
  `icon` VARCHAR(100) DEFAULT NULL COMMENT '图标',
  `cover_image` VARCHAR(500) DEFAULT NULL COMMENT '封面图',
  `description` VARCHAR(500) DEFAULT NULL COMMENT '分类描述',
  `content_count` INT(11) NOT NULL DEFAULT 0 COMMENT '内容数量',
  `sort_order` INT(11) NOT NULL DEFAULT 0 COMMENT '排序号',
  `is_visible` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否可见（0=否, 1=是）',
  `status` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '状态（0=禁用, 1=启用）',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `creator` VARCHAR(64) DEFAULT 'system' COMMENT '创建人',
  `updater` VARCHAR(64) DEFAULT 'system' COMMENT '更新人',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：1已删除/0未删除',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '删除时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_category_code` (`category_code`),
  KEY `idx_parent_code` (`parent_code`),
  KEY `idx_sort_order` (`sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='内容分类';

-- ---------------------------------------------------------------------
-- 3.13.3 content_media 内容多媒体资源（平台共享表，AUTO_INCREMENT）
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `content_media`;
CREATE TABLE `content_media` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `content_code` VARCHAR(64) NOT NULL COMMENT '内容编码',
  `media_type` TINYINT(2) NOT NULL COMMENT '媒体类型（1=图片, 2=视频, 3=音频, 4=附件）',
  `media_url` VARCHAR(500) NOT NULL COMMENT '资源URL',
  `thumbnail_url` VARCHAR(500) DEFAULT NULL COMMENT '缩略图URL',
  `media_name` VARCHAR(200) DEFAULT NULL COMMENT '资源名称',
  `file_format` VARCHAR(20) DEFAULT NULL COMMENT '文件格式',
  `file_size` INT(11) DEFAULT NULL COMMENT '文件大小（KB）',
  `width` INT(11) DEFAULT NULL COMMENT '宽度（像素）',
  `height` INT(11) DEFAULT NULL COMMENT '高度（像素）',
  `duration` INT(11) DEFAULT NULL COMMENT '时长（秒，视频/音频）',
  `media_description` VARCHAR(500) DEFAULT NULL COMMENT '资源描述',
  `is_in_body` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否在正文中（0=否, 1=是）',
  `sort_order` INT(11) NOT NULL DEFAULT 0 COMMENT '排序号',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `creator` VARCHAR(64) DEFAULT 'system' COMMENT '创建人',
  `updater` VARCHAR(64) DEFAULT 'system' COMMENT '更新人',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：1已删除/0未删除',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '删除时间',
  PRIMARY KEY (`id`),
  KEY `idx_content_code` (`content_code`),
  KEY `idx_media_type` (`media_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='内容多媒体资源';

-- ---------------------------------------------------------------------
-- 3.13.4 content_record_share 内容分享记录（分片表，雪花ID）
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `content_record_share`;
CREATE TABLE `content_record_share` (
  `id` BIGINT NOT NULL COMMENT '主键（雪花ID）',
  `content_code` VARCHAR(50) NOT NULL COMMENT '内容编码',
  `sharer_type` VARCHAR(30) NOT NULL COMMENT '分享者类型（agent/client/butler）',
  `sharer_code` VARCHAR(50) NOT NULL COMMENT '分享者编码',
  `share_channel` TINYINT(2) NOT NULL COMMENT '分享渠道（1=微信好友, 2=微信朋友圈, 3=复制链接, 4=二维码）',
  `share_url` VARCHAR(500) DEFAULT NULL COMMENT '分享链接',
  `share_title` VARCHAR(200) DEFAULT NULL COMMENT '分享标题',
  `share_description` VARCHAR(500) DEFAULT NULL COMMENT '分享描述',
  `share_image` VARCHAR(500) DEFAULT NULL COMMENT '分享缩略图',
  `click_count` INT(11) NOT NULL DEFAULT 0 COMMENT '点击次数',
  `convert_count` INT(11) NOT NULL DEFAULT 0 COMMENT '转化次数',
  `share_time` DATETIME NOT NULL COMMENT '分享时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `creator` VARCHAR(64) DEFAULT 'system' COMMENT '创建人',
  `updater` VARCHAR(64) DEFAULT 'system' COMMENT '更新人',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：1已删除/0未删除',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '删除时间',
  PRIMARY KEY (`id`),
  KEY `idx_content_code` (`content_code`),
  KEY `idx_sharer` (`sharer_type`, `sharer_code`),
  KEY `idx_share_time` (`share_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='内容分享记录';

-- ---------------------------------------------------------------------
-- 3.13.5 content_record_read 内容阅读记录（分片表，雪花ID）
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `content_record_read`;
CREATE TABLE `content_record_read` (
  `id` BIGINT NOT NULL COMMENT '主键（雪花ID）',
  `content_code` VARCHAR(50) NOT NULL COMMENT '内容编码',
  `reader_type` VARCHAR(30) NOT NULL COMMENT '阅读者类型（agent/client/butler/guest）',
  `reader_code` VARCHAR(50) DEFAULT NULL COMMENT '阅读者编码',
  `read_duration` INT(11) DEFAULT NULL COMMENT '阅读时长（秒）',
  `read_progress` DECIMAL(5,2) DEFAULT NULL COMMENT '阅读进度（%）',
  `read_source` TINYINT(2) NOT NULL DEFAULT 1 COMMENT '阅读来源（1=自主浏览, 2=分享链接, 3=推荐, 4=搜索）',
  `ip_address` VARCHAR(50) DEFAULT NULL COMMENT 'IP地址',
  `device_type` VARCHAR(20) DEFAULT NULL COMMENT '设备类型',
  `read_time` DATETIME NOT NULL COMMENT '阅读时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `creator` VARCHAR(64) DEFAULT 'system' COMMENT '创建人',
  `updater` VARCHAR(64) DEFAULT 'system' COMMENT '更新人',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：1已删除/0未删除',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '删除时间',
  PRIMARY KEY (`id`),
  KEY `idx_content_code` (`content_code`),
  KEY `idx_reader` (`reader_type`, `reader_code`),
  KEY `idx_read_time` (`read_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='内容阅读记录';
