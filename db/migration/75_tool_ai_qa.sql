SET NAMES utf8mb4;
-- =====================================================================
-- 75_tool_ai_qa.sql  AI 问答工具（你问我答）三表
-- tool_ai_qa_config    人物配置（平台共享，admin 创建）
-- tool_ai_qa_session   会话（按 agent 归属，渠道隔离）
-- tool_ai_qa_message   消息（含引用溯源）
-- =====================================================================

CREATE TABLE IF NOT EXISTS `tool_ai_qa_config` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `config_code` VARCHAR(50) NOT NULL COMMENT '配置编码（QAC+5位序列）',
  `persona_name` VARCHAR(100) NOT NULL COMMENT '人物名称',
  `icon` VARCHAR(100) DEFAULT NULL COMMENT '头像（文字或图标名）',
  `icon_color` VARCHAR(20) DEFAULT 'blue' COMMENT '图标色（blue/green/orange/red/gray）',
  `system_prompt` TEXT NOT NULL COMMENT '人设描述（注入 system prompt）',
  `welcome_msg` VARCHAR(500) DEFAULT NULL COMMENT '开场白/欢迎语',
  `recommend_questions` JSON DEFAULT NULL COMMENT '推荐问题数组',
  `repo_ids` JSON DEFAULT NULL COMMENT '绑定知识库 ID 数组',
  `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序号',
  `status` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '状态（0=禁用 1=启用）',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `creator` VARCHAR(64) DEFAULT 'system' COMMENT '创建人',
  `updater` VARCHAR(64) DEFAULT 'system' COMMENT '更新人',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：1已删除/0未删除',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '删除时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_config_code` (`config_code`),
  KEY `idx_status_sort` (`status`, `sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI 问答人物配置（平台共享）';

CREATE TABLE IF NOT EXISTS `tool_ai_qa_session` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `session_code` VARCHAR(50) NOT NULL COMMENT '会话编码（QAS+5位序列）',
  `config_id` BIGINT NOT NULL COMMENT '所属人物 ID',
  `config_code` VARCHAR(50) NOT NULL COMMENT '人物编码（冗余）',
  `persona_name` VARCHAR(100) NOT NULL COMMENT '人物名（冗余）',
  `agent_code` VARCHAR(50) NOT NULL COMMENT '归属代理人',
  `channel_code` VARCHAR(50) NOT NULL COMMENT '渠道编码（租户隔离）',
  `title` VARCHAR(200) DEFAULT NULL COMMENT '会话标题',
  `message_count` INT NOT NULL DEFAULT 0 COMMENT '消息数',
  `last_message_at` DATETIME DEFAULT NULL COMMENT '最近消息时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：1已删除/0未删除',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '删除时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_session_code` (`session_code`),
  KEY `idx_agent_persona` (`agent_code`, `config_id`),
  KEY `idx_last_msg` (`last_message_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI 问答会话';

CREATE TABLE IF NOT EXISTS `tool_ai_qa_message` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `session_code` VARCHAR(50) NOT NULL COMMENT '所属会话编码',
  `role` VARCHAR(10) NOT NULL COMMENT '角色：user/assistant',
  `content` LONGTEXT NOT NULL COMMENT '消息正文',
  `citations` JSON DEFAULT NULL COMMENT '引用 JSON（assistant 有）',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_session` (`session_code`, `id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI 问答消息';
