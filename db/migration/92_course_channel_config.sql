SET NAMES utf8mb4;
-- =====================================================================
-- 92_course_channel_config.sql  课程渠道配置
--
-- 1. course_info 加 channel_code 字段（渠道课程标识归属，平台课程为 NULL）
-- 2. 新建 channel_config_course 表（渠道课程可见性配置）
-- =====================================================================

-- 1. course_info 加 channel_code 字段
ALTER TABLE `course_info`
  ADD COLUMN `channel_code` VARCHAR(50) DEFAULT NULL
  COMMENT '渠道编码（渠道课程标识归属，平台课程为 NULL）'
  AFTER `remark`;

-- 2. 新建 channel_config_course 表
CREATE TABLE `channel_config_course` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `channel_code` VARCHAR(50) NOT NULL COMMENT '渠道编码',
  `course_code` VARCHAR(50) NOT NULL COMMENT '课程编码（course_info.course_code）',
  `config_type` TINYINT NOT NULL DEFAULT 0 COMMENT '配置类型（0=基础可见性，预留扩展）',
  `config_json` VARCHAR(2000) NOT NULL DEFAULT '{}' COMMENT '配置内容 JSON（预留扩展）',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态（0=禁用 1=启用）',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `creator` VARCHAR(50) DEFAULT NULL COMMENT '创建人',
  `updater` VARCHAR(50) DEFAULT NULL COMMENT '更新人',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：1=已删除，0=未删除',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '删除时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_channel_course_type` (`channel_code`, `course_code`, `config_type`),
  KEY `idx_channel_code` (`channel_code`),
  KEY `idx_course_code` (`course_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='渠道课程配置';
