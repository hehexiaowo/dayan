-- =====================================================================
-- 22_equity_link_refactor.sql  激活链路改造：template_code → goods_code
--
-- 核心变更：
--   1. equity_batch:   template_code → goods_code（批次关联商品而非模板）
--   2. equity_depot:   template_code → goods_code + 删 equity_type/equity_value/use_count/max_use_count + 加 person_count/valid_days 快照
--   3. equity_activate: template_code → goods_code
--   4. service_session: 加 item_code（按服务项目创建会话）
-- =====================================================================

-- ---------------------------------------------------------------------
-- 1. equity_batch: template_code → goods_code
-- ---------------------------------------------------------------------
ALTER TABLE `equity_batch` DROP INDEX `idx_template_code`;
ALTER TABLE `equity_batch` CHANGE COLUMN `template_code` `goods_code` VARCHAR(50) NOT NULL COMMENT '商品编码（关联 goods_info.goods_code）';
ALTER TABLE `equity_batch` ADD KEY `idx_goods_code` (`goods_code`);

-- ---------------------------------------------------------------------
-- 2. equity_depot: template_code → goods_code + 删4字段 + 加2快照字段
-- ---------------------------------------------------------------------
ALTER TABLE `equity_depot` DROP INDEX `idx_template_code`;
ALTER TABLE `equity_depot` CHANGE COLUMN `template_code` `goods_code` VARCHAR(50) NOT NULL COMMENT '商品编码（入库时从批次快照）';
ALTER TABLE `equity_depot` ADD KEY `idx_goods_code` (`goods_code`);

-- 删除被 template 语义驱动的冗余字段（权益类型/面值/使用次数均由 service_item 驱动）
ALTER TABLE `equity_depot` DROP COLUMN `equity_type`;
ALTER TABLE `equity_depot` DROP COLUMN `equity_value`;
-- use_count / max_use_count 语义改为由 service_item 驱动，depot 不再承载
ALTER TABLE `equity_depot` DROP COLUMN `use_count`;
ALTER TABLE `equity_depot` DROP COLUMN `max_use_count`;

-- 新增入库快照字段（从 goods_equity 冻结）
ALTER TABLE `equity_depot` ADD COLUMN `person_count` INT(11) NOT NULL DEFAULT 1 COMMENT '使用人人数快照（入库时从 goods_equity 冻结）' AFTER `goods_code`;
ALTER TABLE `equity_depot` ADD COLUMN `valid_days` INT(11) NOT NULL DEFAULT 365 COMMENT '激活后有效天数快照（入库时从 goods_equity 冻结）' AFTER `person_count`;

-- ---------------------------------------------------------------------
-- 3. equity_activate: template_code → goods_code
-- ---------------------------------------------------------------------
ALTER TABLE `equity_activate` CHANGE COLUMN `template_code` `goods_code` VARCHAR(64) NOT NULL COMMENT '商品编码';

-- ---------------------------------------------------------------------
-- 4. service_session: 加 item_code（按服务项目创建会话时标记来源服务项目）
-- ---------------------------------------------------------------------
ALTER TABLE `service_session` ADD COLUMN `item_code` VARCHAR(50) DEFAULT NULL COMMENT '服务项目编码（权益激活按 service_item 创建会话时标记）' AFTER `equity_code`;
