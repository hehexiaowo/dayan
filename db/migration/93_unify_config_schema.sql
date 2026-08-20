SET NAMES utf8mb4;
-- =====================================================================
-- 93_unify_config_schema.sql  统一渠道配置表 schema
--
-- 给 channel_config_content / channel_config_scene 加 config_json / config_type 字段，
-- 对齐 channel_config_course / channel_config_tool 的可扩展模式。
-- 现有数据自动填充默认值（config_type=0, config_json='{}'），无破坏性。
-- =====================================================================

-- 1. channel_config_content 加 config_type + config_json
ALTER TABLE `channel_config_content`
  ADD COLUMN `config_type` TINYINT NOT NULL DEFAULT 0
  COMMENT '配置类型（0=基础可见性，预留扩展）'
  AFTER `status`,
  ADD COLUMN `config_json` VARCHAR(2000) NOT NULL DEFAULT '{}'
  COMMENT '配置内容 JSON（格式随 config_type 不同）'
  AFTER `config_type`;

-- 2. channel_config_scene 加 config_type + config_json
ALTER TABLE `channel_config_scene`
  ADD COLUMN `config_type` TINYINT NOT NULL DEFAULT 0
  COMMENT '配置类型（0=基础可见性，预留扩展）'
  AFTER `status`,
  ADD COLUMN `config_json` VARCHAR(2000) NOT NULL DEFAULT '{}'
  COMMENT '配置内容 JSON（格式随 config_type 不同）'
  AFTER `config_type`;
