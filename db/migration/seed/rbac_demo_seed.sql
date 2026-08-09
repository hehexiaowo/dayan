SET NAMES utf8mb4;
-- =====================================================================
-- rbac_demo_seed.sql  RBAC 数据权限演示种子
--
-- 演示一个"运营人员"角色（ROLE_OPERATOR）+ 账号 operator/op123，
-- 授权权益/订单/渠道客户/商品/财务流水等业务菜单（不含系统管理、资源管理），
-- 用于端到端验证 /menus/mine 的 RBAC 过滤 + 祖先目录补全 + @SaCheckPermission 鉴权。
--
-- 依赖：admin_seed.sql 已插入 organ_info(OR00001) + 超管 organ_account(AC00001)。
-- 幂等：全部 INSERT ... ON DUPLICATE KEY UPDATE id=id，现有库可重复 source。
-- =====================================================================

-- ========== 1. 运营人员账号 operator / op123（BCrypt strength=10） ==========
-- BCrypt(op123) = $2a$10$icsGaM1ba8fTC5ObMwhQE.38GAvngQO5B/5.QArbFQfTg8bPboMxO
INSERT INTO `organ_account`
  (`organ_code`, `account_code`, `username`, `password`, `salt`,
   `real_name`, `gender`, `phone`, `email`,
   `login_count`, `account_status`, `is_admin`, `remark`,
   `created_at`, `updated_at`, `creator`, `updater`, `deleted`)
VALUES
  ('OR00001', 'AC00002', 'operator',
   '$2a$10$icsGaM1ba8fTC5ObMwhQE.38GAvngQO5B/5.QArbFQfTg8bPboMxO',
   'bcrypt-self-contained',
   '运营演示账号', 0, '13800000001', 'operator@dayanpeng.com',
   0, 1, 0, 'RBAC 数据权限演示账号（密码 op123）',
   NOW(), NOW(), 'system', 'system', 0)
ON DUPLICATE KEY UPDATE `id` = `id`;

-- ========== 2. 运营人员角色 ROLE_OPERATOR ==========
INSERT INTO `organ_role`
  (`organ_code`, `role_code`, `role_name`, `role_type`, `description`,
   `data_scope`, `status`, `sort_order`,
   `created_at`, `updated_at`, `creator`, `updater`, `deleted`)
VALUES
  ('OR00001', 'ROLE_OPERATOR', '运营人员', 2, '业务运营角色：权益/订单/客户/商品/财务（不含系统与资源管理）',
   1, 1, 10,
   NOW(), NOW(), 'system', 'system', 0)
ON DUPLICATE KEY UPDATE `id` = `id`;

-- ========== 3. 账号 ↔ 角色 关联 ==========
INSERT INTO `organ_account_role_rel`
  (`account_code`, `role_code`, `organ_code`,
   `created_at`, `updated_at`, `creator`, `updater`, `deleted`)
VALUES
  ('AC00002', 'ROLE_OPERATOR', 'OR00001',
   NOW(), NOW(), 'system', 'system', 0)
ON DUPLICATE KEY UPDATE `id` = `id`;

-- ========== 4. 角色 ↔ 菜单 关联（17 条：5 父目录 + 12 叶子） ==========
-- 必须包含父目录，否则前端 buildTree 无法重建侧边栏层级
INSERT INTO `organ_role_menu_rel`
  (`role_code`, `menu_code`, `organ_code`,
   `created_at`, `updated_at`, `creator`, `updater`, `deleted`)
VALUES
  -- 工作台（叶子，无父目录）
  ('ROLE_OPERATOR', 'admin_dashboard',     'OR00001', NOW(), NOW(), 'system', 'system', 0),
  -- 权益管理（目录 + 3 叶子）
  ('ROLE_OPERATOR', 'admin_equity',        'OR00001', NOW(), NOW(), 'system', 'system', 0),
  ('ROLE_OPERATOR', 'admin_equity_batch',  'OR00001', NOW(), NOW(), 'system', 'system', 0),
  ('ROLE_OPERATOR', 'admin_equity_depot',  'OR00001', NOW(), NOW(), 'system', 'system', 0),
  -- 订单管理（目录 + 2 叶子）
  ('ROLE_OPERATOR', 'admin_order',         'OR00001', NOW(), NOW(), 'system', 'system', 0),
  ('ROLE_OPERATOR', 'admin_order_equity',  'OR00001', NOW(), NOW(), 'system', 'system', 0),
  ('ROLE_OPERATOR', 'admin_order_scene',   'OR00001', NOW(), NOW(), 'system', 'system', 0),
  -- 渠道管理（目录 + 客户管理）
  ('ROLE_OPERATOR', 'admin_channel',       'OR00001', NOW(), NOW(), 'system', 'system', 0),
  ('ROLE_OPERATOR', 'admin_channel_client','OR00001', NOW(), NOW(), 'system', 'system', 0),
  -- 商品管理（目录 + 2 叶子）
  ('ROLE_OPERATOR', 'admin_goods',             'OR00001', NOW(), NOW(), 'system', 'system', 0),
  ('ROLE_OPERATOR', 'admin_goods_list',        'OR00001', NOW(), NOW(), 'system', 'system', 0),
  ('ROLE_OPERATOR', 'admin_goods_service-item','OR00001', NOW(), NOW(), 'system', 'system', 0),
  -- 财务结算（目录 + 2 叶子）
  ('ROLE_OPERATOR', 'admin_finance',       'OR00001', NOW(), NOW(), 'system', 'system', 0),
  ('ROLE_OPERATOR', 'admin_finance_flow',  'OR00001', NOW(), NOW(), 'system', 'system', 0),
  ('ROLE_OPERATOR', 'admin_finance_bill',  'OR00001', NOW(), NOW(), 'system', 'system', 0),
  -- 管家服务（目录 + 管家列表）
  ('ROLE_OPERATOR', 'admin_service',       'OR00001', NOW(), NOW(), 'system', 'system', 0),
  ('ROLE_OPERATOR', 'admin_service_butler','OR00001', NOW(), NOW(), 'system', 'system', 0)
ON DUPLICATE KEY UPDATE `id` = `id`;

-- ========== 5. 权限定义 + 角色 ↔ 权限 关联（鉴权链路闭环） ==========
-- 不落 organ_permission 的话，DayanStpInterface 返回空权限集，@SaCheckPermission 全部拒绝。
-- 这里只种子叶子菜单对应的 *:list 权限（足够让列表页加载，CRUD 操作权限按需扩展）。
INSERT INTO `organ_permission`
  (`permission_code`, `permission_name`, `parent_code`, `permission_type`,
   `sort_order`, `status`, `remark`,
   `created_at`, `updated_at`, `creator`, `updater`, `deleted`)
VALUES
  ('equity:template:list',  '权益模板列表', NULL, 1, 0, 1, '演示权限', NOW(), NOW(), 'system', 'system', 0),
  ('equity:batch:list',     '权益批次列表', NULL, 1, 0, 1, '演示权限', NOW(), NOW(), 'system', 'system', 0),
  ('equity:depot:list',     '权益仓库列表', NULL, 1, 0, 1, '演示权限', NOW(), NOW(), 'system', 'system', 0),
  ('order:equity:list',     '权益订单列表', NULL, 1, 0, 1, '演示权限', NOW(), NOW(), 'system', 'system', 0),
  ('order:scene:list',      '场景订单列表', NULL, 1, 0, 1, '演示权限', NOW(), NOW(), 'system', 'system', 0),
  ('client:info:list',      '客户列表',     NULL, 1, 0, 1, '演示权限', NOW(), NOW(), 'system', 'system', 0),
  ('goods:info:list',       '商品列表',     NULL, 1, 0, 1, '演示权限', NOW(), NOW(), 'system', 'system', 0),
  ('finance:flow:list',     '财务流水列表', NULL, 1, 0, 1, '演示权限', NOW(), NOW(), 'system', 'system', 0),
  ('finance:bill:list',     '结算单列表',   NULL, 1, 0, 1, '演示权限', NOW(), NOW(), 'system', 'system', 0),
  ('butler:info:list',      '管家列表',     NULL, 1, 0, 1, '演示权限', NOW(), NOW(), 'system', 'system', 0)
ON DUPLICATE KEY UPDATE `id` = `id`;

INSERT INTO `organ_role_permission_ship`
  (`role_code`, `permission_code`,
   `created_at`, `updated_at`, `creator`, `updater`, `deleted`)
VALUES
  ('ROLE_OPERATOR', 'equity:template:list', NOW(), NOW(), 'system', 'system', 0),
  ('ROLE_OPERATOR', 'equity:batch:list',    NOW(), NOW(), 'system', 'system', 0),
  ('ROLE_OPERATOR', 'equity:depot:list',    NOW(), NOW(), 'system', 'system', 0),
  ('ROLE_OPERATOR', 'order:equity:list',    NOW(), NOW(), 'system', 'system', 0),
  ('ROLE_OPERATOR', 'order:scene:list',     NOW(), NOW(), 'system', 'system', 0),
  ('ROLE_OPERATOR', 'client:info:list',     NOW(), NOW(), 'system', 'system', 0),
  ('ROLE_OPERATOR', 'goods:info:list',      NOW(), NOW(), 'system', 'system', 0),
  ('ROLE_OPERATOR', 'finance:flow:list',    NOW(), NOW(), 'system', 'system', 0),
  ('ROLE_OPERATOR', 'finance:bill:list',    NOW(), NOW(), 'system', 'system', 0),
  ('ROLE_OPERATOR', 'butler:info:list',     NOW(), NOW(), 'system', 'system', 0)
ON DUPLICATE KEY UPDATE `id` = `id`;
