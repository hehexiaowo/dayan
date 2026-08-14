-- 强制连接字符集为 utf8mb4，避免 docker-entrypoint-initdb.d 按默认字符集读取
-- 导致中文双重编码（Mojibake）。必须作为第一条语句执行。
SET NAMES utf8mb4;
-- =====================================================================
-- 28_agent_lead.sql  代理人线索表
-- 域说明：代理人 CRM 线索（潜在客户，非真实客户；真实客户在 client_info）
-- 生成依据：2026-08-10-agent-full-redesign-design.md §2.2
-- 主键策略：分片表（雪花ID），含 channel_code，参与渠道分片
-- =====================================================================

-- ---------------------------------------------------------------------
-- agent_lead 代理人线索（分片表，雪花ID）
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `agent_lead`;
CREATE TABLE `agent_lead` (
  `id` BIGINT NOT NULL COMMENT '主键（雪花ID）',
  `lead_code` VARCHAR(50) NOT NULL COMMENT '线索编码（LD+日期+序号，渠道内唯一）',
  `agent_code` VARCHAR(50) NOT NULL COMMENT '归属代理人编码',
  `channel_code` VARCHAR(50) NOT NULL COMMENT '所属渠道编码',
  `name` VARCHAR(100) DEFAULT NULL COMMENT '线索姓名（可能只是称呼）',
  `phone` VARCHAR(20) DEFAULT NULL COMMENT '联系电话',
  `gender` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '性别（0=未知, 1=男, 2=女）',
  `age` INT(11) DEFAULT NULL COMMENT '年龄（可为空）',
  `lead_status` TINYINT(2) NOT NULL DEFAULT 1 COMMENT '线索状态（1=新线索, 2=跟进中, 3=意向, 4=已转化, 5=已流失）',
  `source_type` TINYINT(2) NOT NULL DEFAULT 1 COMMENT '来源类型（1=手工录入, 2=分享扫码, 3=活动接触, 4=转介绍, 5=内容引流）',
  `source_ref` VARCHAR(100) DEFAULT NULL COMMENT '来源溯源（share_code/activity_code/referrer 等）',
  `intention_level` TINYINT(1) DEFAULT NULL COMMENT '意向等级（1=低, 2=中, 3=高）',
  `interest_type` VARCHAR(200) DEFAULT NULL COMMENT '关注养老类型（旅居/活力长居/照护，逗号分隔）',
  `region` VARCHAR(200) DEFAULT NULL COMMENT '关注区域',
  `last_follow_time` DATETIME DEFAULT NULL COMMENT '最后跟进时间',
  `converted_client_code` VARCHAR(50) DEFAULT NULL COMMENT '转化后的客户编码（关联 client_info.client_code）',
  `converted_at` DATETIME DEFAULT NULL COMMENT '转化时间',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `creator` VARCHAR(64) DEFAULT 'system' COMMENT '创建人',
  `updater` VARCHAR(64) DEFAULT 'system' COMMENT '更新人',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：1已删除/0未删除',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '删除时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_channel_lead_code` (`channel_code`, `lead_code`),
  KEY `idx_agent_code` (`agent_code`),
  KEY `idx_lead_status` (`lead_status`),
  KEY `idx_phone` (`phone`),
  KEY `idx_source_type` (`source_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='代理人线索';
