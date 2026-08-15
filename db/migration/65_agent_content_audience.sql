SET NAMES utf8mb4;
-- =====================================================================
-- 65_agent_content_audience.sql  个人内容目标读者字段（v2 效果优化）
-- audience：children=子女决策者 / elder=老人本人 / general=通用
-- 幂等：information_schema 探测，已存在则空操作。
-- =====================================================================
SET @col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'agent_content' AND COLUMN_NAME = 'audience');
SET @ddl = IF(@col_exists = 0,
    'ALTER TABLE `agent_content` ADD COLUMN `audience` VARCHAR(32) DEFAULT NULL COMMENT ''目标读者（children=子女决策者/elder=老人本人/general=通用）'' AFTER `ref_goods_codes`',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
