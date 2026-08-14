SET NAMES utf8mb4;
-- =====================================================================
-- rbac_channel_audit_perm.sql  补齐渠道审核权限码
--
-- channel:info 的 list/query/create/update/delete 已在 rbac_permission_seed.sql
-- 播种（channel 域 30 个权限码）。本次新增渠道审核端点对应的 channel:info:audit。
-- 幂等：ON DUPLICATE KEY UPDATE id=id。超管（is_admin=1）不受影响。
-- =====================================================================
INSERT INTO `organ_permission`
  (`permission_code`, `permission_name`, `parent_code`, `permission_type`, `path`, `method`, `sort_order`, `status`, `remark`, `created_at`, `updated_at`, `creator`, `updater`, `deleted`)
VALUES
  ('channel:info:audit', '渠道审核', 'channel:info', 3, '/admin-api/channels/audit', 'POST', 506, 1, '渠道审核流（待审→通过/驳回）', NOW(), NOW(), 'system', 'system', 0)
ON DUPLICATE KEY UPDATE `id` = `id`;
