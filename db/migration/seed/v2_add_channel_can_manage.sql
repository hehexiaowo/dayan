-- P9 增量2：channel_info 新增 can_manage 列（渠道管理配置能力标记）
SET NAMES utf8mb4;

ALTER TABLE `channel_info`
  ADD COLUMN `can_manage` TINYINT(1) NOT NULL DEFAULT 0
  COMMENT '管理配置能力：0=业务型（仅采购/场景/数据/结算等业务），1=管理型（可建删子渠道+配置app）'
  AFTER `audit_status`;

-- 现有测试根渠道置为管理型
UPDATE `channel_info` SET `can_manage` = 1 WHERE `channel_code` = 'CH00001';
