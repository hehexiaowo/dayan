-- 24_quota_model.sql
-- 配额字段模型：goods_service_item_rel 加 quota_type + service_session 加配额快照字段
--
-- 背景：激活链路原先完全忽略 goods_service_item_rel.quantity，每个 service_item 只创建1个会话。
-- 本次为 service_session 增加配额快照（max_use_count/used_count/quota_type/quota_reset_year），
-- 让"6次/年""1次/终身"这类配额语义在激活时落库、服务完成时计数、年度自动重置。

SET NAMES utf8mb4;

-- -----------------------------------------------------------------------
-- 1. goods_service_item_rel 加 quota_type（配额周期）
--    1=终身总量（长居/照护，按权益有效期），2=年度配额（旅居，每年重置）
-- -----------------------------------------------------------------------
ALTER TABLE `goods_service_item_rel`
  ADD COLUMN `quota_type` TINYINT(1) NOT NULL DEFAULT 2
    COMMENT '配额周期（1=终身总量,2=年度配额）' AFTER `quantity`;

-- -----------------------------------------------------------------------
-- 2. service_session 加配额快照字段（4 列 + 1 索引）
--    激活时从 rel 快照到 session，后续服务完成时 used_count+1
-- -----------------------------------------------------------------------
ALTER TABLE `service_session`
  ADD COLUMN `max_use_count` INT NOT NULL DEFAULT 1
    COMMENT '最大使用次数（激活时从 rel.quantity 快照）' AFTER `touch_count`,
  ADD COLUMN `used_count` INT NOT NULL DEFAULT 0
    COMMENT '已使用次数' AFTER `max_use_count`,
  ADD COLUMN `quota_type` TINYINT(1) NOT NULL DEFAULT 2
    COMMENT '配额周期（1=终身,2=年度）' AFTER `used_count`,
  ADD COLUMN `quota_reset_year` INT NULL
    COMMENT '年度配额当前已重置年份（仅 quota_type=2 使用）' AFTER `quota_type`;

ALTER TABLE `service_session`
  ADD INDEX `idx_quota_reset` (`quota_type`, `quota_reset_year`);
