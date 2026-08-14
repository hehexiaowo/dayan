SET NAMES utf8mb4;
-- =====================================================================
-- 46_display_block_network.sql  展示板块业态维度
-- 控制三种业态（旅居/活力长居/照护长居）详情页各自展示哪些板块；
-- 同一板块可同属多业态。NULL = 全部业态（存量数据零迁移兼容）。
-- =====================================================================
ALTER TABLE `park_display_block`
  ADD COLUMN `network_tags` VARCHAR(64) DEFAULT NULL COMMENT '适用业态（逗号分隔: vital/care/sojourn），空=全部' AFTER `status`;
