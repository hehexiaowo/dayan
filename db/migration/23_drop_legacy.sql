-- 强制连接字符集为 utf8mb4，避免 docker-entrypoint-initdb.d 按默认字符集读取
-- 导致中文双重编码（Mojibake）。必须作为第一条语句执行。
SET NAMES utf8mb4;
-- =====================================================================
-- 23_drop_legacy.sql  废除 equity_template + goods_sku_equity
--
-- 权益模板（equity_template）已被 goods_equity + service_item 架构取代。
-- goods_sku_equity（权益规格子表）已被 goods_equity（1:1 配置表）取代。
-- =====================================================================

DROP TABLE IF EXISTS `goods_sku_equity`;
DROP TABLE IF EXISTS `equity_template`;
