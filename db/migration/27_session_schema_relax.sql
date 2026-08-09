-- 27_session_schema_relax.sql
-- 放宽 service_session + equity_activate 的 NOT NULL 约束（E2E 测试发现）
--
-- 问题：权益激活链路创建 service_session 时不传 service_type（service_item 不直接映射 service_type），
--   而 service_session.service_type 是 NOT NULL → 插入失败。
--   同理 equity_activate.client_phone 在无电话激活场景下也为空。
-- 修复：放宽为允许 NULL，兼容权益激活自动创建会话的场景。

SET NAMES utf8mb4;

ALTER TABLE `service_session` MODIFY COLUMN `service_type` TINYINT NULL DEFAULT NULL;
ALTER TABLE `equity_activate` MODIFY COLUMN `client_phone` VARCHAR(20) NULL DEFAULT NULL;
