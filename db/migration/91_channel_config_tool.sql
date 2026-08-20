SET NAMES utf8mb4;
-- =====================================================================
-- 91_channel_config_tool.sql  渠道工具配置
--
-- 统一渠道工具配置表，对齐 channel_config_content/scene/goods 模式。
-- 所有 tool_info 的渠道级配置统一存此表，不只 aichat 知识库补充。
--
-- config_type=1（问答人物知识库补充）时 config_json 格式：
--   {"repoIds": [1, 2, 3]}
-- =====================================================================
CREATE TABLE `channel_config_tool` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `channel_code` VARCHAR(50) NOT NULL COMMENT '渠道编码',
  `tool_code` VARCHAR(50) NOT NULL COMMENT '工具编码（tool_info.tool_code，TL 前缀）',
  `config_type` TINYINT NOT NULL COMMENT '配置类型（1=问答人物知识库补充）',
  `config_json` VARCHAR(2000) NOT NULL DEFAULT '{}' COMMENT '配置内容 JSON（格式随 config_type 不同）',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `creator` VARCHAR(50) DEFAULT NULL COMMENT '创建人',
  `updater` VARCHAR(50) DEFAULT NULL COMMENT '更新人',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：1=已删除，0=未删除',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '删除时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_channel_tool_type` (`channel_code`, `tool_code`, `config_type`),
  KEY `idx_channel_code` (`channel_code`),
  KEY `idx_tool_code` (`tool_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='渠道工具配置';
