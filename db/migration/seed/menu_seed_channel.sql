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
   'List', 50, 1, 0, 0, 'channel', 1, NOW(), NOW(), 'system', 'system', 0, NULL)
ON DUPLICATE KEY UPDATE `id` = `id`;

-- ========== P9 增量2 追加：系统管理菜单组 ==========
INSERT INTO system_menu
(menu_code, menu_name, parent_code, menu_type, path, component, permission_code,
 icon, sort_order, is_visible, is_external, is_cache, domain_type, status,
 created_at, updated_at, creator, updater, deleted, deleted_at)
VALUES
('channel_system', '系统管理', NULL, 1, '/system', NULL, NULL,
 'Setting', 60, 1, 0, 0, 'channel', 1, NOW(), NOW(), 'system', 'system', 0, NULL),
('channel_system_organ', '架构管理', 'channel_system', 2, '/system/organ', 'system/organ/index', 'channel:info:list',
 'OfficeBuilding', 10, 1, 0, 0, 'channel', 1, NOW(), NOW(), 'system', 'system', 0, NULL),
('channel_system_account', '账号管理', 'channel_system', 2, '/system/account', 'system/account/index', 'channel:account:list',
 'User', 20, 1, 0, 0, 'channel', 1, NOW(), NOW(), 'system', 'system', 0, NULL),
('channel_system_role', '角色管理', 'channel_system', 2, '/system/role', 'system/role/index', 'channel:role:list',
 'UserFilled', 30, 1, 0, 0, 'channel', 1, NOW(), NOW(), 'system', 'system', 0, NULL)
ON DUPLICATE KEY UPDATE `id` = `id`;

-- ========== P9 增量3 追加：采购结算菜单组 ==========
-- 注：channel_order_manage 与增量0 顶层 channel_order 是不同菜单（后者现属"其他业务"），
--     二者 menu_code 不同、parent 不同，并存不冲突。
INSERT INTO system_menu
(menu_code, menu_name, parent_code, menu_type, path, component, permission_code,
 icon, sort_order, is_visible, is_external, is_cache, domain_type, status,
 created_at, updated_at, creator, updater, deleted, deleted_at)
VALUES
('channel_procurement', '采购结算', NULL, 1, '/procurement', NULL, NULL,
 'ShoppingCart', 70, 1, 0, 0, 'channel', 1, NOW(), NOW(), 'system', 'system', 0, NULL),
('channel_mall', '大雁商城', 'channel_procurement', 2, '/mall', 'mall/index', 'channel:goods:list',
 'Goods', 10, 1, 0, 0, 'channel', 1, NOW(), NOW(), 'system', 'system', 0, NULL),
('channel_order_manage', '订单管理', 'channel_procurement', 2, '/order-manage', 'order-manage/index', 'channel:order:list',
 'List', 20, 1, 0, 0, 'channel', 1, NOW(), NOW(), 'system', 'system', 0, NULL),
('channel_invoice', '发票管理', 'channel_procurement', 2, '/invoice', 'invoice/index', 'channel:invoice:list',
 'Document', 30, 1, 0, 0, 'channel', 1, NOW(), NOW(), 'system', 'system', 0, NULL),
('channel_cashier', '收银台', 'channel_procurement', 2, '/cashier', 'cashier/index', 'channel:payment:list',
 'Wallet', 40, 0, 0, 0, 'channel', 1, NOW(), NOW(), 'system', 'system', 0, NULL)
ON DUPLICATE KEY UPDATE `id` = `id`;

-- ========== P9 增量4 追加：养老保典 + 客户平台 ==========
-- channel_agent / channel_client 由叶子（menu_type=2）扩为目录（menu_type=1），
-- 显式覆盖 menu_type/component/path/permission_code 等字段（方案 C 核心）。
-- Channel 端登录即见全量（不按角色过滤），故 menu_type 改动无角色重配副作用。
INSERT INTO system_menu
(menu_code, menu_name, parent_code, menu_type, path, component, permission_code,
 icon, sort_order, is_visible, is_external, is_cache, domain_type, status,
 created_at, updated_at, creator, updater, deleted, deleted_at)
VALUES
-- 养老保典目录（原 channel_agent 改为目录）
('channel_agent', '养老保典', NULL, 1, '/agent', NULL, NULL,
 'User', 20, 1, 0, 0, 'channel', 1, NOW(), NOW(), 'system', 'system', 0, NULL),
('channel_agent_account', '代理人账号', 'channel_agent', 2, '/agent/account', 'agent/account/index', 'channel:agent:list',
 'UserFilled', 21, 1, 0, 0, 'channel', 1, NOW(), NOW(), 'system', 'system', 0, NULL),
('channel_agent_content', '内容配置', 'channel_agent', 2, '/agent/content', 'content/config/index', 'channel:content:list',
 'Document', 22, 1, 0, 0, 'channel', 1, NOW(), NOW(), 'system', 'system', 0, NULL),
('channel_agent_scene', '场景营销', 'channel_agent', 2, '/agent/scene', 'scene/info/index', 'channel:scene:list',
 'Location', 23, 1, 0, 0, 'channel', 1, NOW(), NOW(), 'system', 'system', 0, NULL),
('channel_agent_lead', '客户线索', 'channel_agent', 2, '/agent/lead', 'agent/lead/index', 'channel:agentClient:list',
 'Connection', 24, 1, 0, 0, 'channel', 1, NOW(), NOW(), 'system', 'system', 0, NULL),
('channel_agent_share', '分享记录', 'channel_agent', 2, '/agent/share', 'agent/share/index', 'channel:shareRecord:list',
 'Share', 25, 1, 0, 0, 'channel', 1, NOW(), NOW(), 'system', 'system', 0, NULL),
('channel_agent_read', '阅读记录', 'channel_agent', 2, '/agent/read-record', 'content/read-record/index', 'channel:readRecord:list',
 'View', 26, 1, 0, 0, 'channel', 1, NOW(), NOW(), 'system', 'system', 0, NULL),
-- 客户平台目录（原 channel_client 改为目录）
('channel_client', '客户平台', NULL, 1, '/client', NULL, NULL,
 'UserFilled', 30, 1, 0, 0, 'channel', 1, NOW(), NOW(), 'system', 'system', 0, NULL),
('channel_client_account', '客户账号', 'channel_client', 2, '/client/account', 'client/account/index', 'channel:client:list',
 'User', 31, 1, 0, 0, 'channel', 1, NOW(), NOW(), 'system', 'system', 0, NULL),
('channel_client_content', '内容配置', 'channel_client', 2, '/client/content', 'content/config/index', 'channel:content:list',
 'Document', 32, 1, 0, 0, 'channel', 1, NOW(), NOW(), 'system', 'system', 0, NULL),
('channel_client_activation', '激活记录', 'channel_client', 2, '/client/activation', 'client/activation/index', 'channel:activate:list',
 'Key', 33, 1, 0, 0, 'channel', 1, NOW(), NOW(), 'system', 'system', 0, NULL),
('channel_client_service', '服务记录', 'channel_client', 2, '/client/service', 'service/session/index', 'channel:serviceSession:list',
 'Service', 34, 1, 0, 0, 'channel', 1, NOW(), NOW(), 'system', 'system', 0, NULL)
ON DUPLICATE KEY UPDATE
  menu_name = VALUES(menu_name),
  parent_code = VALUES(parent_code),
  menu_type = VALUES(menu_type),
  path = VALUES(path),
  component = VALUES(component),
  permission_code = VALUES(permission_code),
  icon = VALUES(icon),
  sort_order = VALUES(sort_order),
  is_visible = VALUES(is_visible),
  domain_type = VALUES(domain_type),
  status = VALUES(status);

-- ==================== 增量5 开放平台（1 目录 + 3 子菜单）====================
INSERT INTO `system_menu` (`menu_code`, `menu_name`, `parent_code`, `menu_type`, `path`, `component`, `permission_code`,
 `icon`, `sort_order`, `is_visible`, `is_external`, `is_cache`, `domain_type`, `status`,
 `created_at`, `updated_at`, `creator`, `updater`, `deleted`, `deleted_at`)
VALUES
('channel_open', '开放平台', NULL, 1, '/open', NULL, NULL,
 'Connection', 80, 1, 0, 0, 'channel', 1, NOW(), NOW(), 'system', 'system', 0, NULL),
('channel_open_app', '应用管理', 'channel_open', 2, '/open/app', 'open/app/index', 'channel:openApp:list',
 'Setting', 81, 1, 0, 0, 'channel', 1, NOW(), NOW(), 'system', 'system', 0, NULL),
('channel_open_doc', '接口文档', 'channel_open', 2, '/open/doc', 'open/doc/index', NULL,
 'Document', 82, 1, 0, 0, 'channel', 1, NOW(), NOW(), 'system', 'system', 0, NULL),
('channel_open_guide', '接入指南', 'channel_open', 2, '/open/guide', 'open/guide/index', NULL,
 'Guide', 83, 1, 0, 0, 'channel', 1, NOW(), NOW(), 'system', 'system', 0, NULL)
ON DUPLICATE KEY UPDATE `id` = `id`;
