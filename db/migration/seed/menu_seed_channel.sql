-- 强制连接字符集为 utf8mb4，避免 docker-entrypoint-initdb.d 按默认字符集读取
-- 导致中文双重编码（Mojibake）。必须作为第一条语句执行。
SET NAMES utf8mb4;
-- =====================================================================
-- menu_seed_channel.sql  菜单种子数据（Channel 渠道端）
-- P9 增量 0：工作台 + 现有 4 个业务页（选项 B，见 spec §3.7）。
--            后续增量追加系统管理/采购结算/养老保典/客户平台/开放平台等目录与菜单。
-- domain_type='channel'：Channel 端专属（SystemMenuService.tree("channel") 过滤）。
-- 生成依据：docs/02 §3.1.11 system_menu + .superpowers/specs/2026-08-07-p9-channel-full-design.md §3.4
-- 执行方式：当前库已存在，手动对运行中的 MySQL 执行（docker exec）。
-- =====================================================================

INSERT INTO `system_menu`
  (`menu_code`, `menu_name`, `parent_code`, `menu_type`, `path`, `component`, `permission_code`,
   `icon`, `sort_order`, `is_visible`, `is_external`, `is_cache`, `domain_type`, `status`,
   `created_at`, `updated_at`, `creator`, `updater`, `deleted`, `deleted_at`)
VALUES
  ('channel_dashboard', '工作台', NULL, 2, '/dashboard', 'dashboard/index', 'channel:dashboard:view',
   'Odometer', 10, 1, 0, 0, 'channel', 1, NOW(), NOW(), 'system', 'system', 0, NULL),
  ('channel_agent', '代理人管理', NULL, 2, '/agent', 'agent/index', 'channel:agent:view',
   'User', 20, 1, 0, 0, 'channel', 1, NOW(), NOW(), 'system', 'system', 0, NULL),
  ('channel_client', '客户管理', NULL, 2, '/client', 'client/index', 'channel:client:view',
   'UserFilled', 30, 1, 0, 0, 'channel', 1, NOW(), NOW(), 'system', 'system', 0, NULL),
  ('channel_equity', '权益查询', NULL, 2, '/equity', 'equity/index', 'channel:equity:view',
   'Ticket', 40, 1, 0, 0, 'channel', 1, NOW(), NOW(), 'system', 'system', 0, NULL),
  ('channel_order', '订单查询', NULL, 2, '/order', 'order/index', 'channel:order:view',
   'List', 50, 1, 0, 0, 'channel', 1, NOW(), NOW(), 'system', 'system', 0, NULL);
