SET NAMES utf8mb4;
-- =====================================================================
-- rbac_business_menu.sql  订单/权益/财务 缺失页面菜单补齐
--
-- 背景：
--   rbac_business_perm.sql 已补齐四域按钮级权限码（organ_permission），
--   但 system_menu 中订单(课程/旅居)、权益(激活/换持有人/使用人)、
--   财务(账户/退款/对账/支付) 共 9 个页面的菜单行缺失，
--   导致即便 vue 页面存在也不会出现在侧边栏（后端菜单驱动路由）。
--
--   本文件补齐 9 个 system_menu 行（menu_type=2 叶子菜单），component 对应
--   dayan-admin/src/views 下的 vue 文件路径（动态路由 resolveComponent 据此解析）。
--   超管（is_admin=1）见全部菜单；同时为 ROLE_OPERATOR 补齐同款授权（与既有
--   订单/权益/财务菜单一致）。
--
-- 幂等：system_menu 用 ON DUPLICATE KEY UPDATE；organ_role_menu_rel
--   用 INSERT IGNORE（role_code+menu_code 唯一）。
-- =====================================================================

-- ============================================================
-- 一、订单域：课程订单 + 旅居订单（权益/场景已有）
-- ============================================================
INSERT INTO `system_menu`
  (`menu_code`, `menu_name`, `parent_code`, `menu_type`, `path`, `component`, `permission_code`, `icon`, `sort_order`, `is_visible`, `is_external`, `is_cache`, `domain_type`, `status`, `remark`, `created_at`, `updated_at`, `creator`, `updater`, `deleted`)
VALUES
  ('admin_order_course',   '课程订单', 'admin_order', 2, '/order/course',   'order/course/index',   'order:course:list',   'Tickets', 3, 1, 0, 1, 'admin', 1, '课程订单管理', NOW(), NOW(), 'system', 'system', 0),
  ('admin_order_sojourn',  '旅居订单', 'admin_order', 2, '/order/sojourn',  'order/sojourn/index',  'order:sojourn:list',  'Suitcase', 4, 1, 0, 1, 'admin', 1, '旅居订单管理', NOW(), NOW(), 'system', 'system', 0)
ON DUPLICATE KEY UPDATE `menu_name` = VALUES(`menu_name`), `component` = VALUES(`component`), `permission_code` = VALUES(`permission_code`), `path` = VALUES(`path`), `parent_code` = VALUES(`parent_code`);

-- ============================================================
-- 二、权益域：激活记录 + 换持有人 + 使用人（批次/仓库已有）
-- ============================================================
INSERT INTO `system_menu`
  (`menu_code`, `menu_name`, `parent_code`, `menu_type`, `path`, `component`, `permission_code`, `icon`, `sort_order`, `is_visible`, `is_external`, `is_cache`, `domain_type`, `status`, `remark`, `created_at`, `updated_at`, `creator`, `updater`, `deleted`)
VALUES
  ('admin_equity_activate',     '权益激活',   'admin_equity', 2, '/equity/activate',     'equity/activate/index',     'equity:activate:list',      'CircleCheck', 3, 1, 0, 1, 'admin', 1, '权益激活记录（自动生成，只读）', NOW(), NOW(), 'system', 'system', 0),
  ('admin_equity_change_holder','换持有人',   'admin_equity', 2, '/equity/change-holder','equity/changeHolder/index', 'equity:change-holder:list', 'Switch', 4, 1, 0, 1, 'admin', 1, '换持有人记录（自动生成，只读）', NOW(), NOW(), 'system', 'system', 0),
  ('admin_equity_use_person',   '权益使用人', 'admin_equity', 2, '/equity/use-person',   'equity/usePerson/index',    'equity:use-person:list',    'User', 5, 1, 0, 1, 'admin', 1, '权益使用人管理', NOW(), NOW(), 'system', 'system', 0)
ON DUPLICATE KEY UPDATE `menu_name` = VALUES(`menu_name`), `component` = VALUES(`component`), `permission_code` = VALUES(`permission_code`), `path` = VALUES(`path`), `parent_code` = VALUES(`parent_code`);

-- ============================================================
-- 三、财务域：账户 + 退款 + 对账 + 支付（流水/结算单/发票已有）
-- ============================================================
INSERT INTO `system_menu`
  (`menu_code`, `menu_name`, `parent_code`, `menu_type`, `path`, `component`, `permission_code`, `icon`, `sort_order`, `is_visible`, `is_external`, `is_cache`, `domain_type`, `status`, `remark`, `created_at`, `updated_at`, `creator`, `updater`, `deleted`)
VALUES
  ('admin_finance_account',         '财务账户', 'admin_finance', 2, '/finance/account',        'finance/account/index',         'finance:account:list',         'Wallet', 4, 1, 0, 1, 'admin', 1, '财务账户管理', NOW(), NOW(), 'system', 'system', 0),
  ('admin_finance_refund',          '退款管理', 'admin_finance', 2, '/finance/refund',         'finance/refund/index',          'finance:refund:list',          'RefreshLeft', 5, 1, 0, 1, 'admin', 1, '退款审核与处理', NOW(), NOW(), 'system', 'system', 0),
  ('admin_finance_reconciliation',  '对账管理', 'admin_finance', 2, '/finance/reconciliation', 'finance/reconciliation/index',  'finance:reconciliation:list',  'ScaleToOriginal', 6, 1, 0, 1, 'admin', 1, '财务对账', NOW(), NOW(), 'system', 'system', 0),
  ('admin_finance_payment',         '支付记录', 'admin_finance', 2, '/finance/payment',        'finance/payment/index',         'finance:payment:list',         'CreditCard', 7, 1, 0, 1, 'admin', 1, '支付记录管理', NOW(), NOW(), 'system', 'system', 0)
ON DUPLICATE KEY UPDATE `menu_name` = VALUES(`menu_name`), `component` = VALUES(`component`), `permission_code` = VALUES(`permission_code`), `path` = VALUES(`path`), `parent_code` = VALUES(`parent_code`);

-- ============================================================
-- 四、为 ROLE_OPERATOR 补齐新菜单授权（与既有订单/权益/财务菜单一致）
-- ============================================================
INSERT IGNORE INTO `organ_role_menu_rel`
  (`role_code`, `menu_code`, `organ_code`, `created_at`, `updated_at`, `creator`, `updater`, `deleted`)
VALUES
  ('ROLE_OPERATOR', 'admin_order_course',            NULL, NOW(), NOW(), 'system', 'system', 0),
  ('ROLE_OPERATOR', 'admin_order_sojourn',           NULL, NOW(), NOW(), 'system', 'system', 0),
  ('ROLE_OPERATOR', 'admin_equity_activate',         NULL, NOW(), NOW(), 'system', 'system', 0),
  ('ROLE_OPERATOR', 'admin_equity_change_holder',    NULL, NOW(), NOW(), 'system', 'system', 0),
  ('ROLE_OPERATOR', 'admin_equity_use_person',       NULL, NOW(), NOW(), 'system', 'system', 0),
  ('ROLE_OPERATOR', 'admin_finance_account',         NULL, NOW(), NOW(), 'system', 'system', 0),
  ('ROLE_OPERATOR', 'admin_finance_refund',          NULL, NOW(), NOW(), 'system', 'system', 0),
  ('ROLE_OPERATOR', 'admin_finance_reconciliation',  NULL, NOW(), NOW(), 'system', 'system', 0),
  ('ROLE_OPERATOR', 'admin_finance_payment',         NULL, NOW(), NOW(), 'system', 'system', 0);
