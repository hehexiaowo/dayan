-- 强制连接字符集为 utf8mb4，避免 docker-entrypoint-initdb.d 按默认字符集读取
-- 导致中文双重编码（Mojibake）。必须作为第一条语句执行。
SET NAMES utf8mb4;
-- =====================================================================
-- 39_log_split.sql  系统日志按前端拆分（四端分表，登录与接口操作统一入表）
--
-- 背景：原设计意图即按系统分日志（system_log_organ/supplier/channel），
--   落地时收敛为单表 system_operation_log；现按四端部署现实重拆。
-- 拆分规则（按 ContextHolder.account_type 路由）：
--   system_log_organ   ← admin（含 supplier/distributor/system/unknown 兜底，均无独立端）
--   system_log_channel ← channel
--   system_log_agent   ← agent
--   system_log_client  ← client
-- 登录/登出事件以 module='auth', action='login'/'logout' 写入对应端表，
-- 不再单设 login_log。
--
-- 废除：system_operation_log（通用单表，存量数据按下文分流迁入四端新表后再 DROP）、
--       system_login_log（从未启用）、system_log_supplier（无 supplier 部署端，供应商操作经 admin 落入 organ 表）
-- 重建：system_log_organ / system_log_channel（旧表从未启用且无数据，直接重建为统一 schema）
-- =====================================================================

-- 旧表重建（均无数据，直接 DROP；system_operation_log 有存量数据，留待文末迁移后再 DROP）
DROP TABLE IF EXISTS `system_login_log`;
DROP TABLE IF EXISTS `system_log_supplier`;
DROP TABLE IF EXISTS `system_log_organ`;
DROP TABLE IF EXISTS `system_log_channel`;

-- ---------------------------------------------------------------------
-- system_log_organ 系统日志-管理后台（admin 端全部操作/登录日志）
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `system_log_organ` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `trace_id` VARCHAR(64) DEFAULT NULL COMMENT '链路追踪ID',
  `account_type` VARCHAR(30) NOT NULL DEFAULT 'admin' COMMENT '账号类型（admin/supplier/distributor/system）',
  `account_code` VARCHAR(50) NOT NULL COMMENT '操作账号编码',
  `account_name` VARCHAR(100) DEFAULT NULL COMMENT '操作人姓名',
  `module` VARCHAR(50) NOT NULL COMMENT '操作模块（auth=登录登出）',
  `action` VARCHAR(50) NOT NULL COMMENT '操作动作（login/logout/create/update/delete等）',
  `action_description` VARCHAR(200) DEFAULT NULL COMMENT '操作描述',
  `target_type` VARCHAR(50) DEFAULT NULL COMMENT '操作对象类型',
  `target_code` VARCHAR(50) DEFAULT NULL COMMENT '操作对象编码',
  `target_description` VARCHAR(200) DEFAULT NULL COMMENT '操作对象描述',
  `request_url` VARCHAR(500) DEFAULT NULL COMMENT '请求URL',
  `request_method` VARCHAR(10) DEFAULT NULL COMMENT '请求方法',
  `request_params` TEXT DEFAULT NULL COMMENT '请求参数（脱敏后）',
  `response_result` TEXT DEFAULT NULL COMMENT '响应结果（JSON，超长截断）',
  `response_code` INT(11) DEFAULT NULL COMMENT '响应状态码',
  `ip_address` VARCHAR(50) DEFAULT NULL COMMENT '操作IP地址',
  `ip_location` VARCHAR(200) DEFAULT NULL COMMENT 'IP归属地',
  `user_agent` VARCHAR(500) DEFAULT NULL COMMENT '浏览器UA',
  `device_type` VARCHAR(20) DEFAULT NULL COMMENT '设备类型（pc/mobile/tablet）',
  `os` VARCHAR(50) DEFAULT NULL COMMENT '操作系统',
  `browser` VARCHAR(50) DEFAULT NULL COMMENT '浏览器',
  `result_status` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '结果（0=失败, 1=成功）',
  `error_msg` TEXT DEFAULT NULL COMMENT '错误信息',
  `duration` INT(11) DEFAULT NULL COMMENT '执行耗时（毫秒）',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `creator` VARCHAR(64) DEFAULT 'system' COMMENT '创建人',
  `updater` VARCHAR(64) DEFAULT 'system' COMMENT '更新人',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：1已删除/0未删除',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '删除时间',
  PRIMARY KEY (`id`),
  KEY `idx_trace_id` (`trace_id`),
  KEY `idx_account_code` (`account_code`),
  KEY `idx_module_action` (`module`, `action`),
  KEY `idx_target` (`target_type`, `target_code`),
  KEY `idx_created_at` (`created_at`),
  KEY `idx_ip_address` (`ip_address`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统日志-管理后台';

-- ---------------------------------------------------------------------
-- system_log_channel 系统日志-渠道端
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `system_log_channel` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `trace_id` VARCHAR(64) DEFAULT NULL COMMENT '链路追踪ID',
  `account_type` VARCHAR(30) NOT NULL DEFAULT 'channel' COMMENT '账号类型（channel）',
  `account_code` VARCHAR(50) NOT NULL COMMENT '操作账号编码',
  `account_name` VARCHAR(100) DEFAULT NULL COMMENT '操作人姓名',
  `module` VARCHAR(50) NOT NULL COMMENT '操作模块（auth=登录登出）',
  `action` VARCHAR(50) NOT NULL COMMENT '操作动作（login/logout/create/update/delete等）',
  `action_description` VARCHAR(200) DEFAULT NULL COMMENT '操作描述',
  `target_type` VARCHAR(50) DEFAULT NULL COMMENT '操作对象类型',
  `target_code` VARCHAR(50) DEFAULT NULL COMMENT '操作对象编码',
  `target_description` VARCHAR(200) DEFAULT NULL COMMENT '操作对象描述',
  `request_url` VARCHAR(500) DEFAULT NULL COMMENT '请求URL',
  `request_method` VARCHAR(10) DEFAULT NULL COMMENT '请求方法',
  `request_params` TEXT DEFAULT NULL COMMENT '请求参数（脱敏后）',
  `response_result` TEXT DEFAULT NULL COMMENT '响应结果（JSON，超长截断）',
  `response_code` INT(11) DEFAULT NULL COMMENT '响应状态码',
  `ip_address` VARCHAR(50) DEFAULT NULL COMMENT '操作IP地址',
  `ip_location` VARCHAR(200) DEFAULT NULL COMMENT 'IP归属地',
  `user_agent` VARCHAR(500) DEFAULT NULL COMMENT '浏览器UA',
  `device_type` VARCHAR(20) DEFAULT NULL COMMENT '设备类型（pc/mobile/tablet）',
  `os` VARCHAR(50) DEFAULT NULL COMMENT '操作系统',
  `browser` VARCHAR(50) DEFAULT NULL COMMENT '浏览器',
  `result_status` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '结果（0=失败, 1=成功）',
  `error_msg` TEXT DEFAULT NULL COMMENT '错误信息',
  `duration` INT(11) DEFAULT NULL COMMENT '执行耗时（毫秒）',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `creator` VARCHAR(64) DEFAULT 'system' COMMENT '创建人',
  `updater` VARCHAR(64) DEFAULT 'system' COMMENT '更新人',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：1已删除/0未删除',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '删除时间',
  PRIMARY KEY (`id`),
  KEY `idx_trace_id` (`trace_id`),
  KEY `idx_account_code` (`account_code`),
  KEY `idx_module_action` (`module`, `action`),
  KEY `idx_target` (`target_type`, `target_code`),
  KEY `idx_created_at` (`created_at`),
  KEY `idx_ip_address` (`ip_address`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统日志-渠道端';

-- ---------------------------------------------------------------------
-- system_log_agent 系统日志-代理人端
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `system_log_agent` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `trace_id` VARCHAR(64) DEFAULT NULL COMMENT '链路追踪ID',
  `account_type` VARCHAR(30) NOT NULL DEFAULT 'agent' COMMENT '账号类型（agent）',
  `account_code` VARCHAR(50) NOT NULL COMMENT '操作账号编码',
  `account_name` VARCHAR(100) DEFAULT NULL COMMENT '操作人姓名',
  `module` VARCHAR(50) NOT NULL COMMENT '操作模块（auth=登录登出）',
  `action` VARCHAR(50) NOT NULL COMMENT '操作动作（login/logout/create/update/delete等）',
  `action_description` VARCHAR(200) DEFAULT NULL COMMENT '操作描述',
  `target_type` VARCHAR(50) DEFAULT NULL COMMENT '操作对象类型',
  `target_code` VARCHAR(50) DEFAULT NULL COMMENT '操作对象编码',
  `target_description` VARCHAR(200) DEFAULT NULL COMMENT '操作对象描述',
  `request_url` VARCHAR(500) DEFAULT NULL COMMENT '请求URL',
  `request_method` VARCHAR(10) DEFAULT NULL COMMENT '请求方法',
  `request_params` TEXT DEFAULT NULL COMMENT '请求参数（脱敏后）',
  `response_result` TEXT DEFAULT NULL COMMENT '响应结果（JSON，超长截断）',
  `response_code` INT(11) DEFAULT NULL COMMENT '响应状态码',
  `ip_address` VARCHAR(50) DEFAULT NULL COMMENT '操作IP地址',
  `ip_location` VARCHAR(200) DEFAULT NULL COMMENT 'IP归属地',
  `user_agent` VARCHAR(500) DEFAULT NULL COMMENT '浏览器UA',
  `device_type` VARCHAR(20) DEFAULT NULL COMMENT '设备类型（pc/mobile/tablet）',
  `os` VARCHAR(50) DEFAULT NULL COMMENT '操作系统',
  `browser` VARCHAR(50) DEFAULT NULL COMMENT '浏览器',
  `result_status` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '结果（0=失败, 1=成功）',
  `error_msg` TEXT DEFAULT NULL COMMENT '错误信息',
  `duration` INT(11) DEFAULT NULL COMMENT '执行耗时（毫秒）',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `creator` VARCHAR(64) DEFAULT 'system' COMMENT '创建人',
  `updater` VARCHAR(64) DEFAULT 'system' COMMENT '更新人',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：1已删除/0未删除',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '删除时间',
  PRIMARY KEY (`id`),
  KEY `idx_trace_id` (`trace_id`),
  KEY `idx_account_code` (`account_code`),
  KEY `idx_module_action` (`module`, `action`),
  KEY `idx_target` (`target_type`, `target_code`),
  KEY `idx_created_at` (`created_at`),
  KEY `idx_ip_address` (`ip_address`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统日志-代理人端';

-- ---------------------------------------------------------------------
-- system_log_client 系统日志-客户端
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `system_log_client` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `trace_id` VARCHAR(64) DEFAULT NULL COMMENT '链路追踪ID',
  `account_type` VARCHAR(30) NOT NULL DEFAULT 'client' COMMENT '账号类型（client）',
  `account_code` VARCHAR(50) NOT NULL COMMENT '操作账号编码',
  `account_name` VARCHAR(100) DEFAULT NULL COMMENT '操作人姓名',
  `module` VARCHAR(50) NOT NULL COMMENT '操作模块（auth=登录登出）',
  `action` VARCHAR(50) NOT NULL COMMENT '操作动作（login/logout/create/update/delete等）',
  `action_description` VARCHAR(200) DEFAULT NULL COMMENT '操作描述',
  `target_type` VARCHAR(50) DEFAULT NULL COMMENT '操作对象类型',
  `target_code` VARCHAR(50) DEFAULT NULL COMMENT '操作对象编码',
  `target_description` VARCHAR(200) DEFAULT NULL COMMENT '操作对象描述',
  `request_url` VARCHAR(500) DEFAULT NULL COMMENT '请求URL',
  `request_method` VARCHAR(10) DEFAULT NULL COMMENT '请求方法',
  `request_params` TEXT DEFAULT NULL COMMENT '请求参数（脱敏后）',
  `response_result` TEXT DEFAULT NULL COMMENT '响应结果（JSON，超长截断）',
  `response_code` INT(11) DEFAULT NULL COMMENT '响应状态码',
  `ip_address` VARCHAR(50) DEFAULT NULL COMMENT '操作IP地址',
  `ip_location` VARCHAR(200) DEFAULT NULL COMMENT 'IP归属地',
  `user_agent` VARCHAR(500) DEFAULT NULL COMMENT '浏览器UA',
  `device_type` VARCHAR(20) DEFAULT NULL COMMENT '设备类型（pc/mobile/tablet）',
  `os` VARCHAR(50) DEFAULT NULL COMMENT '操作系统',
  `browser` VARCHAR(50) DEFAULT NULL COMMENT '浏览器',
  `result_status` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '结果（0=失败, 1=成功）',
  `error_msg` TEXT DEFAULT NULL COMMENT '错误信息',
  `duration` INT(11) DEFAULT NULL COMMENT '执行耗时（毫秒）',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `creator` VARCHAR(64) DEFAULT 'system' COMMENT '创建人',
  `updater` VARCHAR(64) DEFAULT 'system' COMMENT '更新人',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：1已删除/0未删除',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '删除时间',
  PRIMARY KEY (`id`),
  KEY `idx_trace_id` (`trace_id`),
  KEY `idx_account_code` (`account_code`),
  KEY `idx_module_action` (`module`, `action`),
  KEY `idx_target` (`target_type`, `target_code`),
  KEY `idx_created_at` (`created_at`),
  KEY `idx_ip_address` (`ip_address`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统日志-客户端';

-- ---------------------------------------------------------------------
-- 存量数据迁移：system_operation_log 按 account_type 分流进四端新表
-- （保留原 id/created_at 等全部列；supplier/distributor/system 兜底进 organ）
-- ---------------------------------------------------------------------
INSERT INTO `system_log_organ` (`id`, `trace_id`, `account_type`, `account_code`, `account_name`,
  `module`, `action`, `action_description`, `target_type`, `target_code`, `target_description`,
  `request_url`, `request_method`, `request_params`, `response_result`, `response_code`,
  `ip_address`, `ip_location`, `user_agent`, `device_type`, `os`, `browser`,
  `result_status`, `error_msg`, `duration`, `created_at`, `updated_at`, `creator`, `updater`, `deleted`, `deleted_at`)
SELECT `id`, `trace_id`, `account_type`, `account_code`, `account_name`,
  `module`, `action`, `action_description`, `target_type`, `target_code`, `target_description`,
  `request_url`, `request_method`, `request_params`, `response_result`, `response_code`,
  `ip_address`, `ip_location`, `user_agent`, `device_type`, `os`, `browser`,
  `result_status`, `error_msg`, `duration`, `created_at`, `updated_at`, `creator`, `updater`, `deleted`, `deleted_at`
FROM `system_operation_log`
WHERE `account_type` NOT IN ('channel', 'agent', 'client');

INSERT INTO `system_log_channel` (`id`, `trace_id`, `account_type`, `account_code`, `account_name`,
  `module`, `action`, `action_description`, `target_type`, `target_code`, `target_description`,
  `request_url`, `request_method`, `request_params`, `response_result`, `response_code`,
  `ip_address`, `ip_location`, `user_agent`, `device_type`, `os`, `browser`,
  `result_status`, `error_msg`, `duration`, `created_at`, `updated_at`, `creator`, `updater`, `deleted`, `deleted_at`)
SELECT `id`, `trace_id`, `account_type`, `account_code`, `account_name`,
  `module`, `action`, `action_description`, `target_type`, `target_code`, `target_description`,
  `request_url`, `request_method`, `request_params`, `response_result`, `response_code`,
  `ip_address`, `ip_location`, `user_agent`, `device_type`, `os`, `browser`,
  `result_status`, `error_msg`, `duration`, `created_at`, `updated_at`, `creator`, `updater`, `deleted`, `deleted_at`
FROM `system_operation_log` WHERE `account_type` = 'channel';

INSERT INTO `system_log_agent` (`id`, `trace_id`, `account_type`, `account_code`, `account_name`,
  `module`, `action`, `action_description`, `target_type`, `target_code`, `target_description`,
  `request_url`, `request_method`, `request_params`, `response_result`, `response_code`,
  `ip_address`, `ip_location`, `user_agent`, `device_type`, `os`, `browser`,
  `result_status`, `error_msg`, `duration`, `created_at`, `updated_at`, `creator`, `updater`, `deleted`, `deleted_at`)
SELECT `id`, `trace_id`, `account_type`, `account_code`, `account_name`,
  `module`, `action`, `action_description`, `target_type`, `target_code`, `target_description`,
  `request_url`, `request_method`, `request_params`, `response_result`, `response_code`,
  `ip_address`, `ip_location`, `user_agent`, `device_type`, `os`, `browser`,
  `result_status`, `error_msg`, `duration`, `created_at`, `updated_at`, `creator`, `updater`, `deleted`, `deleted_at`
FROM `system_operation_log` WHERE `account_type` = 'agent';

INSERT INTO `system_log_client` (`id`, `trace_id`, `account_type`, `account_code`, `account_name`,
  `module`, `action`, `action_description`, `target_type`, `target_code`, `target_description`,
  `request_url`, `request_method`, `request_params`, `response_result`, `response_code`,
  `ip_address`, `ip_location`, `user_agent`, `device_type`, `os`, `browser`,
  `result_status`, `error_msg`, `duration`, `created_at`, `updated_at`, `creator`, `updater`, `deleted`, `deleted_at`)
SELECT `id`, `trace_id`, `account_type`, `account_code`, `account_name`,
  `module`, `action`, `action_description`, `target_type`, `target_code`, `target_description`,
  `request_url`, `request_method`, `request_params`, `response_result`, `response_code`,
  `ip_address`, `ip_location`, `user_agent`, `device_type`, `os`, `browser`,
  `result_status`, `error_msg`, `duration`, `created_at`, `updated_at`, `creator`, `updater`, `deleted`, `deleted_at`
FROM `system_operation_log` WHERE `account_type` = 'client';

-- ---------------------------------------------------------------------
-- 数据落位后废除旧表
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `system_operation_log`;
