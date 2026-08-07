SET NAMES utf8mb4;
-- =====================================================================
-- channel_permission_seed.sql  渠道端 RBAC 权限初始化（P9 增量2）
--
-- 补齐 channel:system 域所有 channel Controller 的权限码，让非超管角色
-- 能执行完整操作（超管 is_admin=1 返 ["*"] 不受影响）。
-- 幂等：ON DUPLICATE KEY UPDATE id=id
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
ON DUPLICATE KEY UPDATE `id` = `id`;
