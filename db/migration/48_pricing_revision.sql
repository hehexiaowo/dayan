SET NAMES utf8mb4;
-- =====================================================================
-- 48_pricing_revision.sql  定价调价预约生效标记（规格 §F）
-- pending_flag=1 的记录由 PricingEffectiveScheduler 在 effective_date 到点
-- 翻转为当前价（旧当前价置 0）。0=无/已生效。
-- =====================================================================
ALTER TABLE `park_pricing`
  ADD COLUMN `pending_flag` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '预约生效标记：1=待生效，0=无/已生效' AFTER `is_current`;

ALTER TABLE `park_pricing`
  ADD INDEX `idx_pending_effective` (`pending_flag`, `effective_date`);
