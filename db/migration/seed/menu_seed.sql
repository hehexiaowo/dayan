-- 强制连接字符集为 utf8mb4，避免 docker-entrypoint-initdb.d 按默认字符集读取
-- 导致中文双重编码（Mojibake）。必须作为第一条语句执行。
SET NAMES utf8mb4;
-- =====================================================================
-- menu_seed.sql  菜单种子数据（Admin 端核心菜单树）
-- P0 阶段初始化 Admin 端主要目录 + 系统管理菜单
-- Channel/Agent/Client 端菜单在各端前端开发时增量补充（domain_type 区分）
-- domain_type 取值：admin/channel/agent/client（区分四端可见性）
-- 生成依据：docs/02 §3.1.11 system_menu
-- =====================================================================

-- ========== Admin 端顶级目录 ==========
INSERT INTO `system_menu`
  (`menu_code`, `menu_name`, `parent_code`, `menu_type`, `path`, `component`, `permission_code`, `icon`, `sort_order`, `is_visible`, `domain_type`, `status`, `remark`, `created_at`, `updated_at`, `creator`, `updater`, `deleted`)
VALUES
  ('admin_dashboard', '首页看板', NULL, 2, '/dashboard', 'dashboard/index', NULL, 'Dashboard', 0, 1, 'admin', 1, 'Admin 工作台', NOW(), NOW(), 'system', 'system', 0),

  ('admin_system', '系统管理', NULL, 1, '/system', NULL, NULL, 'Setting', 10, 1, 'admin', 1, '系统管理目录', NOW(), NOW(), 'system', 'system', 0),
  ('admin_resource', '资源管理', NULL, 1, '/resource', NULL, NULL, 'OfficeBuilding', 20, 1, 'admin', 1, '资源管理目录', NOW(), NOW(), 'system', 'system', 0),
  ('admin_equity', '权益管理', NULL, 1, '/equity', NULL, NULL, 'CreditCard', 30, 1, 'admin', 1, '权益管理目录', NOW(), NOW(), 'system', 'system', 0),
  ('admin_service', '管家服务', NULL, 1, '/service', NULL, NULL, 'Service', 40, 1, 'admin', 1, '管家服务目录', NOW(), NOW(), 'system', 'system', 0),
  ('admin_channel', '渠道管理', NULL, 1, '/channel', NULL, NULL, 'Share', 50, 1, 'admin', 1, '渠道管理目录', NOW(), NOW(), 'system', 'system', 0),
  ('admin_goods', '商品管理', NULL, 1, '/goods', NULL, NULL, 'Goods', 60, 1, 'admin', 1, '商品管理目录', NOW(), NOW(), 'system', 'system', 0),
  ('admin_order', '订单管理', NULL, 1, '/order', NULL, NULL, 'Document', 70, 1, 'admin', 1, '订单管理目录', NOW(), NOW(), 'system', 'system', 0),
  ('admin_finance', '财务结算', NULL, 1, '/finance', NULL, NULL, 'Money', 80, 1, 'admin', 1, '财务结算目录', NOW(), NOW(), 'system', 'system', 0),

  -- ========== 系统管理子菜单 ==========
  ('admin_system_user', '用户管理', 'admin_system', 2, '/system/user', 'system/user/index', 'organ:account:list', 'User', 1, 1, 'admin', 1, '核心账号管理', NOW(), NOW(), 'system', 'system', 0),
  ('admin_system_role', '角色管理', 'admin_system', 2, '/system/role', 'system/role/index', 'organ:role:list', 'UserFilled', 2, 1, 'admin', 1, '角色权限管理', NOW(), NOW(), 'system', 'system', 0),
  ('admin_system_menu', '菜单管理', 'admin_system', 2, '/system/menu', 'system/menu/index', 'system:menu:list', 'Menu', 3, 1, 'admin', 1, '菜单管理', NOW(), NOW(), 'system', 'system', 0),
  ('admin_system_dict', '字典管理', 'admin_system', 2, '/system/dict', 'system/dict/index', 'system:dict:list', 'Collection', 4, 1, 'admin', 1, '字典管理', NOW(), NOW(), 'system', 'system', 0),
  ('admin_system_state_machine', '状态规则', 'admin_system', 2, '/system/state-machine', 'system/stateMachine/index', 'system:sm:list', 'Connection', 5, 1, 'admin', 1, '状态机规则配置', NOW(), NOW(), 'system', 'system', 0),
  ('admin_system_config', '系统配置', 'admin_system', 2, '/system/config', 'system/config/index', 'system:config:list', 'Tools', 6, 1, 'admin', 1, '系统参数配置', NOW(), NOW(), 'system', 'system', 0),
  ('admin_system_log', '操作日志', 'admin_system', 2, '/system/log', 'system/log/index', 'system:log:list', 'Document', 7, 1, 'admin', 1, '操作审计日志', NOW(), NOW(), 'system', 'system', 0),
  ('admin_system_organ', '组织架构', 'admin_system', 2, '/system/organ', 'system/organ/index', 'organ:info:list', 'OfficeBuilding', 8, 1, 'admin', 1, '组织部门管理', NOW(), NOW(), 'system', 'system', 0),

  -- ========== 资源管理子菜单 ==========
  ('admin_resource_park', '养老机构', 'admin_resource', 2, '/resource/park', 'resource/park/index', 'park:info:list', 'House', 1, 1, 'admin', 1, '养老机构管理', NOW(), NOW(), 'system', 'system', 0),
  ('admin_resource_supplier', '供货管理', 'admin_resource', 2, '/resource/supplier', 'resource/supplier/index', 'supplier:info:list', 'Connection', 2, 1, 'admin', 1, '供应商管理', NOW(), NOW(), 'system', 'system', 0),
  ('admin_resource_distributor', '分销管理', 'admin_resource', 2, '/resource/distributor', 'resource/distributor/index', 'distributor:info:list', 'Share', 3, 1, 'admin', 1, '分销商管理', NOW(), NOW(), 'system', 'system', 0),
  ('admin_resource_scene', '场景管理', 'admin_resource', 2, '/resource/scene', 'resource/scene/index', 'scene:info:list', 'Place', 4, 1, 'admin', 1, '场景活动管理', NOW(), NOW(), 'system', 'system', 0),
  ('admin_resource_content', '内容素材', 'admin_resource', 2, '/resource/content', 'resource/content/index', 'content:info:list', 'Picture', 5, 1, 'admin', 1, '内容素材库', NOW(), NOW(), 'system', 'system', 0),
  ('admin_resource_course', '课程管理', 'admin_resource', 2, '/resource/course', 'resource/course/index', 'course:info:list', 'Reading', 6, 1, 'admin', 1, '课程管理', NOW(), NOW(), 'system', 'system', 0),

  -- ========== 权益管理子菜单 ==========
  ('admin_equity_template', '权益模板', 'admin_equity', 2, '/equity/template', 'equity/template/index', 'equity:template:list', 'Tickets', 1, 1, 'admin', 1, '权益模板管理', NOW(), NOW(), 'system', 'system', 0),
  ('admin_equity_batch', '批次管理', 'admin_equity', 2, '/equity/batch', 'equity/batch/index', 'equity:batch:list', 'Files', 2, 1, 'admin', 1, '权益批次管理', NOW(), NOW(), 'system', 'system', 0),
  ('admin_equity_depot', '权益仓库', 'admin_equity', 2, '/equity/depot', 'equity/depot/index', 'equity:depot:list', 'Box', 3, 1, 'admin', 1, '权益卡函库', NOW(), NOW(), 'system', 'system', 0),

  -- ========== 管家服务子菜单 ==========
  ('admin_service_butler', '管家列表', 'admin_service', 2, '/service/butler', 'service/butler/index', 'butler:info:list', 'UserFilled', 1, 1, 'admin', 1, '养老管家管理', NOW(), NOW(), 'system', 'system', 0),
  ('admin_service_session', '服务会话', 'admin_service', 2, '/service/session', 'service/session/index', 'service:session:list', 'ChatLineRound', 2, 1, 'admin', 1, '服务会话查询', NOW(), NOW(), 'system', 'system', 0),

  -- ========== 渠道管理子菜单 ==========
  ('admin_channel_info', '渠道列表', 'admin_channel', 2, '/channel/info', 'channel/info/index', 'channel:info:list', 'Share', 1, 1, 'admin', 1, '渠道信息管理', NOW(), NOW(), 'system', 'system', 0),
  ('admin_channel_agent', '代理管理', 'admin_channel', 2, '/channel/agent', 'channel/agent/index', 'agent:info:list', 'Avatar', 2, 1, 'admin', 1, '代理人管理', NOW(), NOW(), 'system', 'system', 0),
  ('admin_channel_client', '客户管理', 'admin_channel', 2, '/channel/client', 'channel/client/index', 'client:info:list', 'User', 3, 1, 'admin', 1, '客户管理', NOW(), NOW(), 'system', 'system', 0),

  -- ========== 商品管理子菜单 ==========
  ('admin_goods_info', '商品列表', 'admin_goods', 2, '/goods/info', 'goods/info/index', 'goods:info:list', 'Goods', 1, 1, 'admin', 1, '商品 SPU 管理', NOW(), NOW(), 'system', 'system', 0),

  -- ========== 订单管理子菜单 ==========
  ('admin_order_equity', '权益订单', 'admin_order', 2, '/order/equity', 'order/equity/index', 'order:equity:list', 'Document', 1, 1, 'admin', 1, '权益订单', NOW(), NOW(), 'system', 'system', 0),
  ('admin_order_scene', '场景订单', 'admin_order', 2, '/order/scene', 'order/scene/index', 'order:scene:list', 'Document', 2, 1, 'admin', 1, '场景订单', NOW(), NOW(), 'system', 'system', 0),

  -- ========== 财务结算子菜单 ==========
  ('admin_finance_flow', '财务流水', 'admin_finance', 2, '/finance/flow', 'finance/flow/index', 'finance:flow:list', 'Money', 1, 1, 'admin', 1, '财务流水', NOW(), NOW(), 'system', 'system', 0),
  ('admin_finance_bill', '结算单据', 'admin_finance', 2, '/finance/bill', 'finance/bill/index', 'finance:bill:list', 'Tickets', 2, 1, 'admin', 1, '结算单管理', NOW(), NOW(), 'system', 'system', 0),
  ('admin_finance_invoice', '发票管理', 'admin_finance', 2, '/finance/invoice', 'finance/invoice/index', 'finance:invoice:list', 'Document', 3, 1, 'admin', 1, '发票管理', NOW(), NOW(), 'system', 'system', 0);
