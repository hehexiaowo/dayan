-- 强制连接字符集为 utf8mb4，避免 docker-entrypoint-initdb.d 按默认字符集读取
-- 导致中文双重编码（Mojibake）。必须作为第一条语句执行。
SET NAMES utf8mb4;
-- =====================================================================
-- 40_butler_organ_link.sql  管家关联 organ 后台账号
--
-- 背景：养老管家保留独立账号体系（butler_account，面向未来管家端），
--       同时支持为管家开通 organ_account 后台账号，使其可直接登录 admin
--       开展工作；数据权限由 organ_role.data_scope 控制。
--
-- 内容：
--   1. butler_info 增加 account_code 列，关联 organ_account.account_code（可空）
--   2. 预置"养老管家"部门（DEPT_BUTLER）+ 三档管家角色（普通/高级/主管），幂等
--
-- 说明：butler_account / butler_account_role_rel 保留不动；
--       后台账号由服务端在"开通账号"时实时创建（organ_account + organ_employee），
--       本脚本不做存量数据迁移。
-- =====================================================================

-- ---------------------------------------------------------------------
-- 1. butler_info 增加 account_code 关联列
-- ---------------------------------------------------------------------
ALTER TABLE `butler_info`
  ADD COLUMN `account_code` VARCHAR(50) DEFAULT NULL COMMENT '关联后台账号编码（organ_account.account_code，未开通为 NULL）' AFTER `organ_code`,
  ADD KEY `idx_account_code` (`account_code`);

-- ---------------------------------------------------------------------
-- 2. 预置部门 + 角色（幂等：uk_dept_code / uk_role_code 命中即跳过）
-- ---------------------------------------------------------------------
INSERT INTO `organ_department`
  (`organ_code`, `dept_code`, `dept_name`, `parent_code`, `ancestors`, `dept_type`,
   `sort_order`, `status`, `remark`, `created_at`, `updated_at`, `creator`, `updater`, `deleted`)
VALUES
  ('OR00001', 'DEPT_BUTLER', '养老管家', NULL, 'OR00001', 2,
   10, 1, '养老管家专属部门（管家后台账号挂靠）', NOW(), NOW(), 'system', 'system', 0)
ON DUPLICATE KEY UPDATE `updated_at` = `updated_at`;

INSERT INTO `organ_role`
  (`organ_code`, `role_code`, `role_name`, `role_type`, `description`, `data_scope`, `status`, `sort_order`,
   `created_at`, `updated_at`, `creator`, `updater`, `deleted`)
VALUES
  ('OR00001', 'ROLE_BUTLER',        '普通管家', 1, '养老管家（普通）：仅本人数据', 4, 1, 10, NOW(), NOW(), 'system', 'system', 0),
  ('OR00001', 'ROLE_BUTLER_SENIOR', '高级管家', 1, '养老管家（高级）：仅本人数据', 4, 1, 11, NOW(), NOW(), 'system', 'system', 0),
  ('OR00001', 'ROLE_BUTLER_LEADER', '管家主管', 1, '养老管家主管：本部门数据',     3, 1, 12, NOW(), NOW(), 'system', 'system', 0)
ON DUPLICATE KEY UPDATE `updated_at` = `updated_at`;
