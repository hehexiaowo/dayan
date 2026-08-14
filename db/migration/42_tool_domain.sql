-- 强制连接字符集为 utf8mb4，避免 docker-entrypoint-initdb.d 按默认字符集读取
-- 导致中文双重编码（Mojibake）。必须作为第一条语句执行。
SET NAMES utf8mb4;
-- =====================================================================
-- 42_tool_domain.sql  工具域建表 + poster_template 补 DDL
--
-- 背景：
--   1. 获客工具（如 agent 端"退休养老金计算器"）此前硬编码在前端数组里，
--      新增/下架要发版。新建 tool 域统一定义与管理，端上动态拉取。
--   2. poster_template 实体（PosterTemplate）已存在于 agent 模块但从未建表，
--      全新初始化会缺表，本脚本补齐（IF NOT EXISTS，兼容手工建过表的库）。
--
-- 表清单：
--   tool_info        获客工具定义（平台共享表，无 channel_code；
--                    注意已加入 agent 端 dayan.tenant.ignore-tables）
--   poster_template  营销海报模板（雪花ID，平台共享表）
-- =====================================================================

-- ---------------------------------------------------------------------
-- 1. tool_info 获客工具定义
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `tool_info` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tool_code` VARCHAR(50) NOT NULL COMMENT '工具编码（TL+5位序列）',
  `tool_name` VARCHAR(100) NOT NULL COMMENT '工具名称',
  `tool_type` TINYINT(2) NOT NULL DEFAULT 1 COMMENT '工具类型（1=计算器, 2=测评, 3=表单, 4=其他）',
  `tool_desc` VARCHAR(500) DEFAULT NULL COMMENT '工具简介',
  `icon` VARCHAR(100) DEFAULT NULL COMMENT '图标（文字或图标名）',
  `entry_path` VARCHAR(200) NOT NULL COMMENT '入口路径（端上页面路径）',
  `config` JSON DEFAULT NULL COMMENT '工具配置（JSON，按工具自定义；如 {"color":"orange"}）',
  `visible_scope` VARCHAR(50) NOT NULL DEFAULT 'agent' COMMENT '可见端（逗号分隔：agent/client）',
  `sort_order` INT(11) NOT NULL DEFAULT 0 COMMENT '排序号（越小越靠前）',
  `status` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '状态（0=禁用, 1=启用）',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `creator` VARCHAR(64) DEFAULT 'system' COMMENT '创建人',
  `updater` VARCHAR(64) DEFAULT 'system' COMMENT '更新人',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：1已删除/0未删除',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '删除时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tool_code` (`tool_code`),
  KEY `idx_status_sort` (`status`, `sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='获客工具定义';

-- 预置 agent 端现有两个计算器（与 pages/acquisition/tools 页硬编码保持一致）
INSERT INTO `tool_info`
  (`tool_code`, `tool_name`, `tool_type`, `tool_desc`, `icon`, `entry_path`, `config`,
   `visible_scope`, `sort_order`, `status`, `remark`,
   `created_at`, `updated_at`, `creator`, `updater`, `deleted`)
VALUES
  ('TL00001', '退休养老金计算器', 1, '根据当前工资、缴费年限，估算退休后每月可领养老金',
   '退', '/pages/acquisition/tools/pension-calculator', JSON_OBJECT('color', 'orange'),
   'agent', 1, 1, '预置：agent 端获客工具',
   NOW(), NOW(), 'system', 'system', 0),
  ('TL00002', '养老生活缺口计算器', 1, '计算退休资金缺口，帮客户提前做好养老储备规划',
   '缺', '/pages/acquisition/tools/gap-calculator', JSON_OBJECT('color', 'red'),
   'agent', 2, 1, '预置：agent 端获客工具',
   NOW(), NOW(), 'system', 'system', 0)
ON DUPLICATE KEY UPDATE `updated_at` = `updated_at`;

-- ---------------------------------------------------------------------
-- 2. poster_template 营销海报模板（补齐缺失 DDL；实体早已存在于 agent 模块）
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `poster_template` (
  `id` BIGINT NOT NULL COMMENT '主键（雪花ID）',
  `template_code` VARCHAR(50) NOT NULL COMMENT '模板编码（PT+yyyyMMdd+seq）',
  `title` VARCHAR(200) NOT NULL COMMENT '标题',
  `subtitle` VARCHAR(200) DEFAULT NULL COMMENT '副标题',
  `body_text` TEXT COMMENT '营销正文',
  `cover_image` VARCHAR(500) DEFAULT NULL COMMENT '封面/背景图',
  `category_code` VARCHAR(50) DEFAULT NULL COMMENT '分类编码',
  `category_name` VARCHAR(100) DEFAULT NULL COMMENT '分类名称',
  `sort_order` INT(11) NOT NULL DEFAULT 0 COMMENT '排序号',
  `status` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '状态（0=禁用, 1=启用）',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `creator` VARCHAR(64) DEFAULT 'system' COMMENT '创建人',
  `updater` VARCHAR(64) DEFAULT 'system' COMMENT '更新人',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：1已删除/0未删除',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '删除时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_template_code` (`template_code`),
  KEY `idx_status_sort` (`status`, `sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='营销海报模板';
