SET NAMES utf8mb4;
-- =====================================================================
-- 71_agent_content_purpose.sql  个人内容文章目的字段
-- purpose：product=产品宣传 / park=机构推荐 / science=科普获客
-- 幂等：information_schema 探测，已存在则空操作（照抄 65 的模式）。
-- =====================================================================
SET @col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'agent_content' AND COLUMN_NAME = 'purpose');
SET @ddl = IF(@col_exists = 0,
    'ALTER TABLE `agent_content` ADD COLUMN `purpose` VARCHAR(32) DEFAULT NULL COMMENT ''文章目的（product/park/science）'' AFTER `audience`',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
