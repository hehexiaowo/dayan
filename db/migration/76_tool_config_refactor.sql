SET NAMES utf8mb4;
-- =====================================================================
-- 76_tool_config_refactor.sql  工具配置重构
-- 1) tool_info.config → config_json；tool_type 改为四类固定字符串
-- 2) 预置四类实例名称同步；补 TL00004 你问我答
-- 3) tool_ai_creator → tool_ai_creator_record + tool_code
-- 4) 问答会话补 tool_code
-- 5) 新增两个计算器记录表
-- 6) admin 菜单「获客工具」改名「工具配置」
-- =====================================================================

-- ---------------------------------------------------------------------
-- 1. tool_info：列改名 + 类型改字符串
-- ---------------------------------------------------------------------
ALTER TABLE `tool_info`
  CHANGE COLUMN `config` `config_json` JSON DEFAULT NULL COMMENT '工具配置 JSON（按类型承载提示词/默认值等）';

ALTER TABLE `tool_info`
  MODIFY COLUMN `tool_type` VARCHAR(32) NOT NULL DEFAULT 'pension' COMMENT '工具类型：pension/gap/ai_creator/ai_qa';

UPDATE `tool_info` SET `tool_type` = 'pension'    WHERE `tool_code` = 'TL00001';
UPDATE `tool_info` SET `tool_type` = 'gap'        WHERE `tool_code` = 'TL00002';
UPDATE `tool_info` SET `tool_type` = 'ai_creator' WHERE `tool_code` = 'TL00003';
UPDATE `tool_info` SET `tool_name` = '社保养老计算器', `tool_desc` = '根据当前工资、缴费年限，估算退休后每月可领养老金'
  WHERE `tool_code` = 'TL00001';
UPDATE `tool_info` SET `tool_name` = '养老缺口计算器', `tool_desc` = '计算退休资金缺口，帮客户提前做好养老储备规划'
  WHERE `tool_code` = 'TL00002';

INSERT INTO `tool_info`
  (`tool_code`, `tool_name`, `tool_type`, `tool_desc`, `icon`, `entry_path`, `config_json`,
   `visible_scope`, `sort_order`, `status`, `remark`,
   `created_at`, `updated_at`, `creator`, `updater`, `deleted`)
VALUES
  ('TL00004', '你问我答', 'ai_qa', '基于知识库的 AI 问答：选人物、带引用、保留对话',
   '答', '/pages/acquisition/qa/index', JSON_OBJECT('color', 'red'),
   'agent', 4, 1, '预置：agent 端问答工具',
   NOW(), NOW(), 'system', 'system', 0)
ON DUPLICATE KEY UPDATE
  `tool_name` = VALUES(`tool_name`),
  `tool_type` = VALUES(`tool_type`),
  `entry_path` = VALUES(`entry_path`),
  `updated_at` = NOW();

-- ---------------------------------------------------------------------
-- 2. AI 创作记录表改名 + 补 tool_code
-- ---------------------------------------------------------------------
RENAME TABLE `tool_ai_creator` TO `tool_ai_creator_record`;

ALTER TABLE `tool_ai_creator_record`
  ADD COLUMN `tool_code` VARCHAR(50) NOT NULL DEFAULT 'TL00003' COMMENT '所属工具实例（tool_info.tool_code）' AFTER `id`;

ALTER TABLE `tool_ai_creator_record`
  ADD KEY `idx_tool_code` (`tool_code`);

ALTER TABLE `tool_ai_creator_record` COMMENT = 'AI 创作使用记录（按 tool_code 区分公众号/小红书等实例）';

-- ---------------------------------------------------------------------
-- 3. 你问我答会话补 tool_code
-- ---------------------------------------------------------------------
ALTER TABLE `tool_ai_qa_session`
  ADD COLUMN `tool_code` VARCHAR(50) NOT NULL DEFAULT 'TL00004' COMMENT '所属工具实例（tool_info.tool_code）' AFTER `id`;

ALTER TABLE `tool_ai_qa_session`
  ADD KEY `idx_tool_code` (`tool_code`);

-- ---------------------------------------------------------------------
-- 4. 社保养老计算器记录
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `tool_pension_calculator_record` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `record_code` VARCHAR(50) NOT NULL COMMENT '记录编码（TPC+5位序列）',
  `tool_code` VARCHAR(50) NOT NULL COMMENT '所属工具实例',
  `agent_code` VARCHAR(50) NOT NULL COMMENT '操作代理人',
  `channel_code` VARCHAR(50) NOT NULL COMMENT '渠道编码',
  `input_json` JSON DEFAULT NULL COMMENT '计算输入',
  `result_json` JSON DEFAULT NULL COMMENT '计算结果',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `creator` VARCHAR(64) DEFAULT 'system' COMMENT '创建人',
  `updater` VARCHAR(64) DEFAULT 'system' COMMENT '更新人',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：1已删除/0未删除',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '删除时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_record_code` (`record_code`),
  KEY `idx_tool_agent` (`tool_code`, `agent_code`),
  KEY `idx_channel` (`channel_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='社保养老计算器使用记录';

-- ---------------------------------------------------------------------
-- 5. 养老缺口计算器记录
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `tool_gap_calculator_record` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `record_code` VARCHAR(50) NOT NULL COMMENT '记录编码（TGC+5位序列）',
  `tool_code` VARCHAR(50) NOT NULL COMMENT '所属工具实例',
  `agent_code` VARCHAR(50) NOT NULL COMMENT '操作代理人',
  `channel_code` VARCHAR(50) NOT NULL COMMENT '渠道编码',
  `input_json` JSON DEFAULT NULL COMMENT '计算输入',
  `result_json` JSON DEFAULT NULL COMMENT '计算结果',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `creator` VARCHAR(64) DEFAULT 'system' COMMENT '创建人',
  `updater` VARCHAR(64) DEFAULT 'system' COMMENT '更新人',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：1已删除/0未删除',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '删除时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_record_code` (`record_code`),
  KEY `idx_tool_agent` (`tool_code`, `agent_code`),
  KEY `idx_channel` (`channel_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='养老缺口计算器使用记录';

-- ---------------------------------------------------------------------
-- 6. 菜单改名
-- ---------------------------------------------------------------------
UPDATE `system_menu`
   SET `menu_name` = '工具配置',
       `remark` = '四个内置工具类型的实例配置',
       `updated_at` = NOW()
 WHERE `menu_code` = 'admin_resource_tool';
