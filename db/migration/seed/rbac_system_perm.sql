SET NAMES utf8mb4;
-- =====================================================================
-- rbac_system_perm.sql  系统管理域菜单补充 + 按钮级权限补齐
--
-- 内容：
--   1. 新增「业务字典」独立管理菜单（挂 admin_system 下）；
--   2. 补齐 system 域全部按钮级权限码（dict/ dict-biz/ sm/ config 的 create/update/delete）。
--      此前仅 :list 菜单级权限被播种，非超管角色对配置/状态机/字典的增删改全部 403。
--
-- 幂等：system_menu 用 ON DUPLICATE KEY UPDATE；organ_permission 用 ON DUPLICATE KEY
--   UPDATE id=id。超管（is_admin=1）走通配 "*" 不受影响。
-- =====================================================================

-- ============================================================
-- 一、新增菜单：业务字典
-- ============================================================
INSERT INTO `system_menu`
  (`menu_code`, `menu_name`, `parent_code`, `menu_type`, `path`, `component`, `permission_code`, `icon`, `sort_order`, `is_visible`, `domain_type`, `status`, `remark`, `created_at`, `updated_at`, `creator`, `updater`, `deleted`)
VALUES
  ('admin_system_dict_business', '业务字典', 'admin_system', 2, '/system/dict-business', 'system/dictBusiness/index', 'system:dict-biz:list', 'Operation', 5, 1, 'admin', 1, '业务字典管理（按业务域）', NOW(), NOW(), 'system', 'system', 0)
ON DUPLICATE KEY UPDATE `menu_name` = VALUES(`menu_name`), `component` = VALUES(`component`), `permission_code` = VALUES(`permission_code`), `path` = VALUES(`path`);

-- ============================================================
-- 二、system 域权限码补齐（dict / dict-biz / sm / config）
-- ============================================================
INSERT INTO `organ_permission`
  (`permission_code`, `permission_name`, `parent_code`, `permission_type`, `path`, `method`, `sort_order`, `status`, `remark`, `created_at`, `updated_at`, `creator`, `updater`, `deleted`)
VALUES
  -- system:dict（字典 CRUD）
  ('system:dict:query',  '字典项详情', 'system:dict', 3, '/admin-api/dicts/*/*', 'GET',    11, 1, '字典管理', NOW(), NOW(), 'system', 'system', 0),
  ('system:dict:create', '新增字典项', 'system:dict', 3, '/admin-api/dicts',     'POST',   12, 1, '字典管理', NOW(), NOW(), 'system', 'system', 0),
  ('system:dict:update', '修改字典项', 'system:dict', 3, '/admin-api/dicts/*',   'PUT',    13, 1, '字典管理', NOW(), NOW(), 'system', 'system', 0),
  ('system:dict:delete', '删除字典项', 'system:dict', 3, '/admin-api/dicts/*',   'DELETE', 14, 1, '字典管理', NOW(), NOW(), 'system', 'system', 0),
  -- system:dict-biz（业务字典 CRUD）
  ('system:dict-biz:list',   '业务字典列表', 'system:dict-biz', 3, '/admin-api/dicts-business',   'GET',    20, 1, '业务字典', NOW(), NOW(), 'system', 'system', 0),
  ('system:dict-biz:create', '新增业务字典', 'system:dict-biz', 3, '/admin-api/dicts-business',   'POST',   21, 1, '业务字典', NOW(), NOW(), 'system', 'system', 0),
  ('system:dict-biz:update', '修改业务字典', 'system:dict-biz', 3, '/admin-api/dicts-business/*', 'PUT',    22, 1, '业务字典', NOW(), NOW(), 'system', 'system', 0),
  ('system:dict-biz:delete', '删除业务字典', 'system:dict-biz', 3, '/admin-api/dicts-business/*', 'DELETE', 23, 1, '业务字典', NOW(), NOW(), 'system', 'system', 0),
  -- system:sm（状态机 CRUD）
  ('system:sm:create', '新增状态规则', 'system:sm', 3, '/admin-api/state-machines',   'POST',   31, 1, '状态规则', NOW(), NOW(), 'system', 'system', 0),
  ('system:sm:update', '修改状态规则', 'system:sm', 3, '/admin-api/state-machines/*', 'PUT',    32, 1, '状态规则', NOW(), NOW(), 'system', 'system', 0),
  ('system:sm:delete', '删除状态规则', 'system:sm', 3, '/admin-api/state-machines/*', 'DELETE', 33, 1, '状态规则', NOW(), NOW(), 'system', 'system', 0),
  -- system:config（配置 CRUD）
  ('system:config:create', '新增配置', 'system:config', 3, '/admin-api/configs',   'POST',   41, 1, '系统配置', NOW(), NOW(), 'system', 'system', 0),
  ('system:config:update', '修改配置', 'system:config', 3, '/admin-api/configs/*', 'PUT',    42, 1, '系统配置', NOW(), NOW(), 'system', 'system', 0),
  ('system:config:delete', '删除配置', 'system:config', 3, '/admin-api/configs/*', 'DELETE', 43, 1, '系统配置', NOW(), NOW(), 'system', 'system', 0)
ON DUPLICATE KEY UPDATE `id` = `id`;
