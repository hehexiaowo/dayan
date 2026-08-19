SET NAMES utf8mb4;
-- =====================================================================
-- channel_permission_seed.sql  渠道端 RBAC 权限初始化（P9 增量2）
--
-- 补齐 channel:system 域所有 channel Controller 的权限码，让非超管角色
-- 能执行完整操作（超管 is_admin=1 返 ["*"] 不受影响）。
-- 幂等：ON DUPLICATE KEY UPDATE `updated_at` = `updated_at`
--
-- 注：channel_permission 表实际无 remark 列（与任务简报 SQL 有差异），
--     此处按运行库实际结构对齐（permission_code 唯一键）。
-- =====================================================================

INSERT INTO `channel_permission`
  (`permission_code`, `permission_name`, `parent_code`, `permission_type`,
   `path`, `method`, `sort_order`, `status`,
   `created_at`, `updated_at`, `creator`, `updater`, `deleted`)
VALUES
  -- 系统管理目录节点（permission_type=1 菜单）
  ('channel:system', '系统管理', NULL, 1, NULL, NULL, 0, 1, NOW(), NOW(), 'system', 'system', 0),
  -- 架构管理 channel:info
  ('channel:info:list',   '架构列表',   'channel:system', 3, '/channel-api/channel-infos',        'GET',    100, 1, NOW(), NOW(), 'system', 'system', 0),
  ('channel:info:tree',   '架构树',     'channel:system', 3, '/channel-api/channel-infos/tree',   'GET',    101, 1, NOW(), NOW(), 'system', 'system', 0),
  ('channel:info:create', '新建子渠道', 'channel:system', 3, '/channel-api/channel-infos',        'POST',   102, 1, NOW(), NOW(), 'system', 'system', 0),
  ('channel:info:update', '编辑渠道',   'channel:system', 3, '/channel-api/channel-infos/*',      'PUT',    103, 1, NOW(), NOW(), 'system', 'system', 0),
  ('channel:info:delete', '删除子渠道', 'channel:system', 3, '/channel-api/channel-infos/*',      'DELETE', 104, 1, NOW(), NOW(), 'system', 'system', 0),
  -- 账号管理 channel:account
  ('channel:account:list',          '账号列表',   'channel:system', 3, '/channel-api/channel-accounts',                   'GET',    110, 1, NOW(), NOW(), 'system', 'system', 0),
  ('channel:account:query',         '账号详情',   'channel:system', 3, '/channel-api/channel-accounts/*',                 'GET',    111, 1, NOW(), NOW(), 'system', 'system', 0),
  ('channel:account:create',        '新建账号',   'channel:system', 3, '/channel-api/channel-accounts',                   'POST',   112, 1, NOW(), NOW(), 'system', 'system', 0),
  ('channel:account:update',        '编辑账号',   'channel:system', 3, '/channel-api/channel-accounts/*',                 'PUT',    113, 1, NOW(), NOW(), 'system', 'system', 0),
  ('channel:account:delete',        '删除账号',   'channel:system', 3, '/channel-api/channel-accounts/*',                 'DELETE', 114, 1, NOW(), NOW(), 'system', 'system', 0),
  ('channel:account:reset',      '重置密码',   'channel:system', 3, '/channel-api/channel-accounts/*/reset-password',  'PUT',    115, 1, NOW(), NOW(), 'system', 'system', 0),
  ('channel:account:assign',     '分配角色',   'channel:system', 3, '/channel-api/channel-account-roles/*/roles',      'PUT',    116, 1, NOW(), NOW(), 'system', 'system', 0),
  -- 角色管理 channel:role
  ('channel:role:list',             '角色列表',   'channel:system', 3, '/channel-api/channel-roles',                'GET',    120, 1, NOW(), NOW(), 'system', 'system', 0),
  ('channel:role:query',            '角色详情',   'channel:system', 3, '/channel-api/channel-roles/*',              'GET',    121, 1, NOW(), NOW(), 'system', 'system', 0),
  ('channel:role:create',           '新建角色',   'channel:system', 3, '/channel-api/channel-roles',                'POST',   122, 1, NOW(), NOW(), 'system', 'system', 0),
  ('channel:role:update',           '编辑角色',   'channel:system', 3, '/channel-api/channel-roles/*',              'PUT',    123, 1, NOW(), NOW(), 'system', 'system', 0),
  ('channel:role:delete',           '删除角色',   'channel:system', 3, '/channel-api/channel-roles/*',              'DELETE', 124, 1, NOW(), NOW(), 'system', 'system', 0),
  ('channel:role:assign',         '角色授权',   'channel:system', 3, '/channel-api/channel-roles/*/permissions',  'PUT',    125, 1, NOW(), NOW(), 'system', 'system', 0),
  -- 权限只读 channel:permission
  ('channel:permission:list', '权限列表', 'channel:system', 3, '/channel-api/channel-permissions/all', 'GET', 130, 1, NOW(), NOW(), 'system', 'system', 0)
ON DUPLICATE KEY UPDATE `updated_at` = `updated_at`;

-- ========== P9 增量3 追加：采购结算域权限码 ==========
-- 5 节点（1 目录 + 4 子菜单）+ 11 接口码 = 16 条
-- 覆盖任务1-3 Controller 的 @SaCheckPermission：
--   订单 3 类（Scene/Course/Sojourn）：channel:order:list/query/cancel（权益订单增量1 无注解，本期 4 类订单均不下单，故无 create）
--   支付单 ChannelFinanceController：channel:payment:list/query/create
--   发票 ChannelInvoiceController：channel:invoice:list/query/apply
--   商品（增量1 复用）：channel:goods:list/query
INSERT INTO `channel_permission`
  (`permission_code`, `permission_name`, `parent_code`, `permission_type`,
   `path`, `method`, `sort_order`, `status`,
   `created_at`, `updated_at`, `creator`, `updater`, `deleted`)
VALUES
  -- 采购结算目录
  ('channel:procurement', '采购结算', NULL, 1, NULL, NULL, 70, 1, NOW(), NOW(), 'system', 'system', 0),
  -- 商城节点 channel:goods（复用增量1 goods-infos Controller）
  ('channel:goods',         '商城',   'channel:procurement', 2, NULL, NULL, 10, 1, NOW(), NOW(), 'system', 'system', 0),
  ('channel:goods:list',    '商品列表', 'channel:goods',     3, '/channel-api/goods-infos',    'GET', 11, 1, NOW(), NOW(), 'system', 'system', 0),
  ('channel:goods:query',   '商品详情', 'channel:goods',     3, '/channel-api/goods-infos/*', 'GET', 12, 1, NOW(), NOW(), 'system', 'system', 0),
  -- 订单节点 channel:order（4 类订单共享）
  ('channel:order',         '订单',   'channel:procurement', 2, NULL, NULL, 20, 1, NOW(), NOW(), 'system', 'system', 0),
  ('channel:order:list',    '订单列表', 'channel:order',     3, '/channel-api/order-*',           'GET',  21, 1, NOW(), NOW(), 'system', 'system', 0),
  ('channel:order:query',   '订单详情', 'channel:order',     3, '/channel-api/order-*/**',        'GET',  22, 1, NOW(), NOW(), 'system', 'system', 0),
  ('channel:order:cancel',  '取消订单', 'channel:order',     3, '/channel-api/order-*/**/cancel', 'POST', 23, 1, NOW(), NOW(), 'system', 'system', 0),
  -- 发票节点 channel:invoice
  ('channel:invoice',         '发票',     'channel:procurement', 2, NULL, NULL, 30, 1, NOW(), NOW(), 'system', 'system', 0),
  ('channel:invoice:list',    '发票列表', 'channel:invoice',     3, '/channel-api/finance-invoices',        'GET',  31, 1, NOW(), NOW(), 'system', 'system', 0),
  ('channel:invoice:query',   '发票详情', 'channel:invoice',     3, '/channel-api/finance-invoices/*',      'GET',  32, 1, NOW(), NOW(), 'system', 'system', 0),
  ('channel:invoice:apply',   '申请发票', 'channel:invoice',     3, '/channel-api/finance-invoices/apply',  'POST', 33, 1, NOW(), NOW(), 'system', 'system', 0),
  -- 支付节点 channel:payment
  ('channel:payment',         '支付',       'channel:procurement', 2, NULL, NULL, 40, 1, NOW(), NOW(), 'system', 'system', 0),
  ('channel:payment:list',    '支付单列表', 'channel:payment',     3, '/channel-api/finance-payments',   'GET',  41, 1, NOW(), NOW(), 'system', 'system', 0),
  ('channel:payment:query',   '支付单详情', 'channel:payment',     3, '/channel-api/finance-payments/*', 'GET',  42, 1, NOW(), NOW(), 'system', 'system', 0),
  ('channel:payment:create',  '创建支付',   'channel:payment',     3, '/channel-api/finance-payments',   'POST', 43, 1, NOW(), NOW(), 'system', 'system', 0)
ON DUPLICATE KEY UPDATE `updated_at` = `updated_at`;

-- ========== P9 增量4 追加：养老保典 + 客户平台 权限码 ==========
-- 10 组权限码（含 9 个一级目录 + 各自 list/query，外加 contentConfig 3 条）= 31 条
-- 覆盖任务1-3 Controller 的 @SaCheckPermission：
--   代理人账号 / 客户线索 / 分享记录 / 内容 / 阅读记录 / 场景营销 / 客户账号 / 激活记录 / 服务记录 / 配置自管
-- 注：channel:agent / channel:client 若增量1 已 seed，ON DUPLICATE KEY UPDATE `updated_at` = `updated_at` 是空更新，保留原值。
INSERT INTO `channel_permission`
  (`permission_code`, `permission_name`, `parent_code`, `permission_type`,
   `path`, `method`, `sort_order`, `status`,
   `created_at`, `updated_at`, `creator`, `updater`, `deleted`)
VALUES
  -- 代理人账号
  ('channel:agent', '代理人账号', NULL, 1, NULL, NULL, 80, 1, NOW(), NOW(), 'system', 'system', 0),
  ('channel:agent:list', '代理人账号列表', 'channel:agent', 3, '/channel-api/agent-accounts', 'GET', 81, 1, NOW(), NOW(), 'system', 'system', 0),
  ('channel:agent:query', '代理人账号详情', 'channel:agent', 3, '/channel-api/agent-accounts/*', 'GET', 82, 1, NOW(), NOW(), 'system', 'system', 0),
  -- 客户线索
  ('channel:agentClient', '客户线索', NULL, 1, NULL, NULL, 90, 1, NOW(), NOW(), 'system', 'system', 0),
  ('channel:agentClient:list', '客户线索列表', 'channel:agentClient', 3, '/channel-api/agent-client-rels', 'GET', 91, 1, NOW(), NOW(), 'system', 'system', 0),
  -- 注：本期 AgentClientRel 仅 page（无 getDetail），不配 :query 详情权限码，避免死权限码
  -- 分享记录
  ('channel:shareRecord', '分享记录', NULL, 1, NULL, NULL, 100, 1, NOW(), NOW(), 'system', 'system', 0),
  ('channel:shareRecord:list', '分享记录列表', 'channel:shareRecord', 3, '/channel-api/agent-share-records', 'GET', 101, 1, NOW(), NOW(), 'system', 'system', 0),
  -- 注：本期 AgentShareRecord 仅 page（无 getDetail），不配 :query 详情权限码
  -- 内容（agent+client 共用）
  ('channel:content', '内容', NULL, 1, NULL, NULL, 110, 1, NOW(), NOW(), 'system', 'system', 0),
  ('channel:content:list', '内容列表', 'channel:content', 3, '/channel-api/contents', 'GET', 111, 1, NOW(), NOW(), 'system', 'system', 0),
  ('channel:content:query', '内容详情', 'channel:content', 3, '/channel-api/contents/*', 'GET', 112, 1, NOW(), NOW(), 'system', 'system', 0),
  -- 阅读记录
  ('channel:readRecord', '阅读记录', NULL, 1, NULL, NULL, 120, 1, NOW(), NOW(), 'system', 'system', 0),
  ('channel:readRecord:list', '阅读记录列表', 'channel:readRecord', 3, '/channel-api/content-read-records', 'GET', 121, 1, NOW(), NOW(), 'system', 'system', 0),
  -- 注：本期 ContentReadRecord 仅 page（无 getDetail），不配 :query 详情权限码
  -- 场景营销
  ('channel:scene', '场景营销', NULL, 1, NULL, NULL, 130, 1, NOW(), NOW(), 'system', 'system', 0),
  ('channel:scene:list', '场景列表', 'channel:scene', 3, '/channel-api/scenes', 'GET', 131, 1, NOW(), NOW(), 'system', 'system', 0),
  ('channel:scene:query', '场景详情', 'channel:scene', 3, '/channel-api/scenes/*', 'GET', 132, 1, NOW(), NOW(), 'system', 'system', 0),
  -- 客户账号
  ('channel:client', '客户账号', NULL, 1, NULL, NULL, 140, 1, NOW(), NOW(), 'system', 'system', 0),
  ('channel:client:list', '客户账号列表', 'channel:client', 3, '/channel-api/client-accounts', 'GET', 141, 1, NOW(), NOW(), 'system', 'system', 0),
  -- 注：本期 ClientAccount 仅 page（Service 无公开 getDetail），不配 :query 详情权限码
  -- 激活记录
  ('channel:activate', '激活记录', NULL, 1, NULL, NULL, 150, 1, NOW(), NOW(), 'system', 'system', 0),
  ('channel:activate:list', '激活记录列表', 'channel:activate', 3, '/channel-api/equity-activates', 'GET', 151, 1, NOW(), NOW(), 'system', 'system', 0),
  -- 注：本期 EquityActivate 仅 page（无 getDetail），不配 :query 详情权限码
  -- 服务记录
  ('channel:serviceSession', '服务记录', NULL, 1, NULL, NULL, 160, 1, NOW(), NOW(), 'system', 'system', 0),
  ('channel:serviceSession:list', '服务记录列表', 'channel:serviceSession', 3, '/channel-api/service-sessions', 'GET', 161, 1, NOW(), NOW(), 'system', 'system', 0),
  ('channel:serviceSession:query', '服务记录详情', 'channel:serviceSession', 3, '/channel-api/service-sessions/*', 'GET', 162, 1, NOW(), NOW(), 'system', 'system', 0),
  -- 内容配置自管
  ('channel:contentConfig', '配置自管', NULL, 1, NULL, NULL, 170, 1, NOW(), NOW(), 'system', 'system', 0),
  ('channel:contentConfig:list', '查配置', 'channel:contentConfig', 3, '/channel-api/channel-configs/*', 'GET', 171, 1, NOW(), NOW(), 'system', 'system', 0),
  ('channel:contentConfig:save', '保存配置', 'channel:contentConfig', 3, '/channel-api/channel-configs/*', 'PUT', 172, 1, NOW(), NOW(), 'system', 'system', 0)
ON DUPLICATE KEY UPDATE `updated_at` = `updated_at`;

-- ==================== 增量5 开放平台（1 组 2 条）====================
INSERT INTO `channel_permission` (`permission_code`, `permission_name`, `parent_code`, `permission_type`,
 `path`, `method`, `sort_order`, `status`,
 `created_at`, `updated_at`, `creator`, `updater`, `deleted`)
VALUES
('channel:openApp', '应用管理', NULL, 1, NULL, NULL, 180, 1, NOW(), NOW(), 'system', 'system', 0),
('channel:openApp:list', '应用配置查看', 'channel:openApp', 3, '/channel-api/open-platforms', 'GET', 181, 1, NOW(), NOW(), 'system', 'system', 0)
ON DUPLICATE KEY UPDATE `updated_at` = `updated_at`;

-- ==================== 知识仓库（本渠道知识库管理，parent=channel:system）====================
-- 对应 ChannelKnowledgeController（/channel-api/system/knowledge/repos/*，dayan-module-knowledge）。
-- 8 条接口码：查看/创建/更新/删除/同步/上传文档/删除文档/问答。
INSERT INTO `channel_permission` (`permission_code`, `permission_name`, `parent_code`, `permission_type`,
 `path`, `method`, `sort_order`, `status`,
 `created_at`, `updated_at`, `creator`, `updater`, `deleted`)
VALUES
('channel:knowledge:view',      '知识仓库查看',   'channel:system', 3, '/channel-api/system/knowledge/repos/**',            'GET',    200, 1, NOW(), NOW(), 'system', 'system', 0),
('channel:knowledge:create',    '创建知识仓库',   'channel:system', 3, '/channel-api/system/knowledge/repos',               'POST',   201, 1, NOW(), NOW(), 'system', 'system', 0),
('channel:knowledge:update',    '编辑知识仓库',   'channel:system', 3, '/channel-api/system/knowledge/repos/*',             'PUT',    202, 1, NOW(), NOW(), 'system', 'system', 0),
('channel:knowledge:delete',    '删除知识仓库',   'channel:system', 3, '/channel-api/system/knowledge/repos/*',             'DELETE', 203, 1, NOW(), NOW(), 'system', 'system', 0),
('channel:knowledge:sync',      '同步知识仓库',   'channel:system', 3, '/channel-api/system/knowledge/repos/*/sync',        'POST',   204, 1, NOW(), NOW(), 'system', 'system', 0),
('channel:knowledge:doc:upload','上传文档',       'channel:system', 3, '/channel-api/system/knowledge/repos/*/documents',   'POST',   205, 1, NOW(), NOW(), 'system', 'system', 0),
('channel:knowledge:doc:delete','删除文档',       'channel:system', 3, '/channel-api/system/knowledge/repos/*/documents/*','DELETE', 206, 1, NOW(), NOW(), 'system', 'system', 0),
('channel:knowledge:chat',      '知识库问答',     'channel:system', 3, '/channel-api/system/knowledge/repos/*/chat',        'POST',   207, 1, NOW(), NOW(), 'system', 'system', 0)
ON DUPLICATE KEY UPDATE `updated_at` = `updated_at`;

-- ========== 增量：问答人物（渠道补充知识库）权限码 ==========
-- 对应 ChannelToolAichatController（/channel-api/tools/aichat/*，dayan-module-tool）。
INSERT INTO `channel_permission`
  (`permission_code`, `permission_name`, `parent_code`, `permission_type`,
   `path`, `method`, `sort_order`, `status`,
   `created_at`, `updated_at`, `creator`, `updater`, `deleted`)
VALUES
  ('channel:tool:aichat:view',   '问答人物查看',     'channel:system', 3, '/channel-api/tools/aichat/**',                 'GET',  210, 1, NOW(), NOW(), 'system', 'system', 0),
  ('channel:tool:aichat:update', '问答人物补充库',   'channel:system', 3, '/channel-api/tools/aichat/personas/*/repos',   'PUT',  211, 1, NOW(), NOW(), 'system', 'system', 0)
ON DUPLICATE KEY UPDATE `updated_at` = `updated_at`;

