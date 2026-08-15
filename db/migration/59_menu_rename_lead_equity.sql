SET NAMES utf8mb4;
-- =====================================================================
-- 59_menu_rename_lead_equity.sql  菜单更名（运营口径统一）
--   渠道管理/线索池   → 线索记录（admin_channel_lead）
--   权益管理/权益使用人 → 权益人员（admin_equity_use_person）
-- 仅改菜单显示名，路径/组件/权限码不动。
-- 全量源 seed/rbac_resource_perm.sql、seed/rbac_business_menu.sql 已同步
-- （两者 ODKU 会以 VALUES 覆盖 menu_name，必须同步否则种子重跑回滚改名）。
-- 幂等：按 menu_code 定位，重复执行结果不变。
-- =====================================================================

UPDATE `system_menu` SET `menu_name` = '线索记录', `remark` = '访客线索记录（分享追踪自动建档，只读）', `updated_at` = NOW()
 WHERE `menu_code` = 'admin_channel_lead';

UPDATE `system_menu` SET `menu_name` = '权益人员', `remark` = '权益人员管理', `updated_at` = NOW()
 WHERE `menu_code` = 'admin_equity_use_person';

-- 权限注册表显示名同步（权限码不动，仅角色授权树的展示文案）
UPDATE `organ_permission` SET `permission_name` = '权益人员列表', `updated_at` = NOW()
 WHERE `permission_code` = 'equity:use-person:list' AND `permission_name` = '权益使用人列表';
UPDATE `organ_permission` SET `permission_name` = '权益人员详情', `updated_at` = NOW()
 WHERE `permission_code` = 'equity:use-person:query' AND `permission_name` = '权益使用人详情';
UPDATE `organ_permission` SET `permission_name` = '设为默认权益人员', `updated_at` = NOW()
 WHERE `permission_code` = 'equity:use-person:set-default' AND `permission_name` = '设为默认使用人';
