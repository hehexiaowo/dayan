SET NAMES utf8mb4;
-- =====================================================================
-- 91_tool_channel_repo_bind.sql  渠道问答人物补充知识库绑定
--
-- 你问我答人物（tool_info 的 aichat 实例）知识库两层模型：
--   admin 全局绑定存 tool_info.config_json.repoIds；
--   渠道补充存本表（按人物分别补充，并集生效）。
-- 运行时有效库 = 全局 repoIds ∪ 本表查询结果（去重保序）。
-- =====================================================================
CREATE TABLE `tool_channel_repo_bind` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `tool_code` VARCHAR(50) NOT NULL COMMENT '问答人物编码（tool_info.tool_code）',
  `channel_code` VARCHAR(50) NOT NULL COMMENT '补充方渠道编码（ContextHolder 注入）',
  `repo_id` BIGINT NOT NULL COMMENT '补充的知识库 ID（system_knowledge_repo.id）',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `creator` VARCHAR(50) DEFAULT NULL COMMENT '创建人',
  `updater` VARCHAR(50) DEFAULT NULL COMMENT '更新人',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：1=已删除，0=未删除',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '删除时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tool_channel_repo` (`tool_code`, `channel_code`, `repo_id`),
  KEY `idx_channel_code` (`channel_code`),
  KEY `idx_tool_code` (`tool_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='渠道问答人物补充知识库绑定';
