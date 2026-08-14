-- 强制连接字符集为 utf8mb4，避免 docker-entrypoint-initdb.d 按默认字符集读取
-- 导致中文双重编码（Mojibake）。必须作为第一条语句执行。
SET NAMES utf8mb4;
-- =====================================================================
-- 32_lead_trace.sql  线索互动追踪
-- 1) ALTER agent_lead — 加访客追踪列（visitor_token / 微信信息 / 互动统计）
-- 2) CREATE agent_lead_trace — 互动明细表（浏览内容/使用工具/查看海报）
-- =====================================================================

-- ---------------------------------------------------------------------
-- 1. ALTER agent_lead
-- ---------------------------------------------------------------------
ALTER TABLE `agent_lead`
  ADD COLUMN `visitor_token` VARCHAR(64) DEFAULT NULL COMMENT '访客令牌（匿名唯一标识，UUID）' AFTER `source_ref`,
  ADD COLUMN `visitor_source` VARCHAR(20) DEFAULT NULL COMMENT '访客来源（wechat/browser/unknown）' AFTER `visitor_token`,
  ADD COLUMN `wx_nickname` VARCHAR(100) DEFAULT NULL COMMENT '微信昵称' AFTER `visitor_source`,
  ADD COLUMN `wx_avatar` VARCHAR(500) DEFAULT NULL COMMENT '微信头像URL' AFTER `wx_nickname`,
  ADD COLUMN `last_trace_time` DATETIME DEFAULT NULL COMMENT '最后互动时间' AFTER `wx_avatar`,
  ADD COLUMN `trace_count` INT NOT NULL DEFAULT 0 COMMENT '互动总次数' AFTER `last_trace_time`,
  ADD INDEX `idx_visitor_token` (`visitor_token`),
  ADD INDEX `idx_last_trace_time` (`last_trace_time`);

-- ---------------------------------------------------------------------
-- 2. CREATE agent_lead_trace  线索互动记录
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `agent_lead_trace` (
  `id` BIGINT NOT NULL COMMENT '主键（雪花ID）',
  `lead_id` BIGINT NOT NULL COMMENT '关联 agent_lead.id',
  `agent_code` VARCHAR(50) NOT NULL COMMENT '代理人编码',
  `channel_code` VARCHAR(50) NOT NULL COMMENT '渠道编码',
  `trace_type` TINYINT NOT NULL COMMENT '互动类型（1=浏览内容, 2=使用工具, 3=查看海报）',
  `biz_code` VARCHAR(50) NOT NULL COMMENT '业务编码（contentCode/toolKey/templateCode）',
  `biz_title` VARCHAR(200) DEFAULT NULL COMMENT '展示标题',
  `trace_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '互动时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `creator` VARCHAR(64) DEFAULT 'system' COMMENT '创建人',
  `updater` VARCHAR(64) DEFAULT 'system' COMMENT '更新人',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：1已删除/0未删除',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '删除时间',
  PRIMARY KEY (`id`),
  KEY `idx_lead_id` (`lead_id`),
  KEY `idx_agent_code` (`agent_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='线索互动记录';
