-- 强制连接字符集为 utf8mb4，避免 docker-entrypoint-initdb.d 按默认字符集读取
-- 导致中文双重编码（Mojibake）。必须作为第一条语句执行。
SET NAMES utf8mb4;
-- =====================================================================
-- 38_rename_records.sql  变更类表命名归位（log → record，并归入所属业务域）
--
-- 语义规约：以人工/接口操作留下的审计流水归 system_log_*（见 39）；
--           业务对象自身的变更历史是"业务记录"，归所属业务域并以 _record 结尾。
-- 1) system_service_change_log → service_change_record（移入 service 域）
-- 2) system_order_status_log   → order_status_change_record（移入 order 域）
-- 3) system_config_log         → system_config_change_record（留 system 域）
-- 4) channel_data_sync_log     → channel_open_sync_record（留 channel 域，配套 channel_open_platform）
-- 仅改表名与表注释，字段不变。
-- =====================================================================

RENAME TABLE `system_service_change_log` TO `service_change_record`;
RENAME TABLE `system_order_status_log`   TO `order_status_change_record`;
RENAME TABLE `system_config_log`         TO `system_config_change_record`;
RENAME TABLE `channel_data_sync_log`     TO `channel_open_sync_record`;

ALTER TABLE `service_change_record`       COMMENT='服务变更记录';
ALTER TABLE `order_status_change_record`  COMMENT='订单状态变更记录';
ALTER TABLE `system_config_change_record` COMMENT='系统配置变更记录';
ALTER TABLE `channel_open_sync_record`    COMMENT='渠道开放平台同步记录';
