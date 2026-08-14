SET NAMES utf8mb4;
-- =====================================================================
-- 49_content_network.sql  内容业态维度（规格 §G.2）
-- 控制三种业态（旅居/活力长居/照护长居）C 端内容流各自展示哪些内容；
-- NULL = 全部业态（存量数据零迁移兼容）。
-- =====================================================================
ALTER TABLE `content_info`
  ADD COLUMN `network_tags` VARCHAR(64) DEFAULT NULL COMMENT '适用业态（逗号分隔: vital/care/sojourn），空=全部' AFTER `tags`;
