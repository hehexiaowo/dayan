SET NAMES utf8mb4;
-- =====================================================================
-- rbac_organ_menu_perm.sql  organ 域菜单重组 + 按钮级权限补齐
--
-- 背景：
--   1. 原 admin_basic_organ「组织架构」菜单实际渲染部门页，且权限码
--      organ:info:list 与部门控制器实际使用的 organ:dept:* 不一致（非超管
--      看到菜单却 403）。本次拆为「机构管理」+「部门管理」两个菜单。
--   2. organ 域按钮级权限（query/create/update/delete/assign/reset/status）
--      此前仅在 controller 用 @SaCheckPermission 引用，未在 organ_permission
--      播种，导致非超管角色（is_admin=0）无法被授予这些权限。
--
-- 幂等：system_menu 用 ON DUPLICATE KEY UPDATE；organ_permission 用
--   ON DUPLICATE KEY UPDATE id=id。现有库可重复 source。
--   超管账号（is_admin=1）走通配权限 "*"，不受影响。
-- =====================================================================

-- ============================================================
-- 一、菜单重组：admin_basic_organ 改为「机构管理」，新增「部门管理」
-- ============================================================
UPDATE `system_menu`
SET `menu_name` = '机构管理', `remark` = '机构信息管理', `icon` = 'OfficeBuilding', `sort_order` = 4
WHERE `menu_code` = 'admin_basic_organ';

-- 部门管理菜单：权限码 organ:dept:list 与 OrganDepartmentAdminController 实际鉴权码对齐
INSERT INTO `system_menu`
  (`menu_code`, `menu_name`, `parent_code`, `menu_type`, `path`, `component`, `permission_code`, `icon`, `sort_order`, `is_visible`, `domain_type`, `status`, `remark`, `created_at`, `updated_at`, `creator`, `updater`, `deleted`)
VALUES
  ('admin_basic_department', '部门管理', 'admin_basic', 2, '/basic/department', 'basic/department/index', 'organ:dept:list', 'Share', 5, 1, 'admin', 1, '部门树管理', NOW(), NOW(), 'system', 'system', 0)
ON DUPLICATE KEY UPDATE `menu_name` = VALUES(`menu_name`), `component` = VALUES(`component`), `permission_code` = VALUES(`permission_code`), `path` = VALUES(`path`), `icon` = VALUES(`icon`);

-- ============================================================
-- 二、补齐 organ 域按钮级权限（非超管可授权）
-- ============================================================
INSERT INTO `organ_permission`
  (`permission_code`, `permission_name`, `parent_code`, `permission_type`,
   `path`, `method`,
   `sort_order`, `status`, `remark`,
   `created_at`, `updated_at`, `creator`, `updater`, `deleted`)
VALUES
  -- organ:account（账号，含 reset/status/assign 特殊动作）
  ('organ:account:query',  '账号详情',     'organ:account', 3, '/admin-api/accounts/*',             'GET',    11, 1, '账号管理', NOW(), NOW(), 'system', 'system', 0),
  ('organ:account:create', '新增账号',     'organ:account', 3, '/admin-api/accounts',              'POST',   12, 1, '账号管理', NOW(), NOW(), 'system', 'system', 0),
  ('organ:account:update', '修改账号',     'organ:account', 3, '/admin-api/accounts/*',            'PUT',    13, 1, '账号管理', NOW(), NOW(), 'system', 'system', 0),
  ('organ:account:delete', '删除账号',     'organ:account', 3, '/admin-api/accounts/*',            'DELETE', 14, 1, '账号管理', NOW(), NOW(), 'system', 'system', 0),
  ('organ:account:reset',  '重置账号密码', 'organ:account', 3, '/admin-api/accounts/*/reset-password', 'PUT', 15, 1, '账号管理', NOW(), NOW(), 'system', 'system', 0),
  ('organ:account:status', '账号状态切换', 'organ:account', 3, '/admin-api/accounts/*/status/*',   'PUT',    16, 1, '账号管理', NOW(), NOW(), 'system', 'system', 0),
  ('organ:account:assign', '账号分配角色', 'organ:account', 3, '/admin-api/account-roles/*/roles', 'PUT',    17, 1, '账号管理', NOW(), NOW(), 'system', 'system', 0),
  -- organ:role（角色，含 assign 权限授权）
  ('organ:role:query',  '角色详情',     'organ:role', 3, '/admin-api/roles/*',             'GET',    11, 1, '角色管理', NOW(), NOW(), 'system', 'system', 0),
  ('organ:role:create', '新增角色',     'organ:role', 3, '/admin-api/roles',              'POST',   12, 1, '角色管理', NOW(), NOW(), 'system', 'system', 0),
  ('organ:role:update', '修改角色',     'organ:role', 3, '/admin-api/roles/*',            'PUT',    13, 1, '角色管理', NOW(), NOW(), 'system', 'system', 0),
  ('organ:role:delete', '删除角色',     'organ:role', 3, '/admin-api/roles/*',            'DELETE', 14, 1, '角色管理', NOW(), NOW(), 'system', 'system', 0),
  ('organ:role:assign', '角色分配权限', 'organ:role', 3, '/admin-api/roles/*/permissions', 'PUT',   15, 1, '角色管理', NOW(), NOW(), 'system', 'system', 0),
  -- organ:info（机构）
  ('organ:info:query',  '机构详情', 'organ:info', 3, '/admin-api/organs/*', 'GET',    11, 1, '机构管理', NOW(), NOW(), 'system', 'system', 0),
  ('organ:info:create', '新增机构', 'organ:info', 3, '/admin-api/organs',  'POST',   12, 1, '机构管理', NOW(), NOW(), 'system', 'system', 0),
  ('organ:info:update', '修改机构', 'organ:info', 3, '/admin-api/organs/*', 'PUT',    13, 1, '机构管理', NOW(), NOW(), 'system', 'system', 0),
  ('organ:info:delete', '删除机构', 'organ:info', 3, '/admin-api/organs/*', 'DELETE', 14, 1, '机构管理', NOW(), NOW(), 'system', 'system', 0),
  -- organ:dept（部门，主键 organCode+deptCode 联合）
  ('organ:dept:list',   '部门列表', 'organ:dept', 3, '/admin-api/departments',     'GET',    10, 1, '部门管理', NOW(), NOW(), 'system', 'system', 0),
  ('organ:dept:create', '新增部门', 'organ:dept', 3, '/admin-api/departments',     'POST',   11, 1, '部门管理', NOW(), NOW(), 'system', 'system', 0),
  ('organ:dept:update', '修改部门', 'organ:dept', 3, '/admin-api/departments/*/*', 'PUT',    12, 1, '部门管理', NOW(), NOW(), 'system', 'system', 0),
  ('organ:dept:delete', '删除部门', 'organ:dept', 3, '/admin-api/departments/*/*', 'DELETE', 13, 1, '部门管理', NOW(), NOW(), 'system', 'system', 0)
ON DUPLICATE KEY UPDATE `id` = `id`;
