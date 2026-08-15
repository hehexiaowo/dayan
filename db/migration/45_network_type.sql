SET NAMES utf8mb4;
-- =====================================================================
-- 45_network_type.sql  业态字典（与 NetworkType 枚举一一对应）
-- vital=活力长居 / care=照护长居 / sojourn=旅游短居
-- =====================================================================
INSERT INTO `system_dict_business`
  (`dict_type`, `dict_code`, `dict_name`, `dict_value`, `domain`, `sort_order`, `status`, `remark`,
   `created_at`, `updated_at`, `creator`, `updater`, `deleted`)
VALUES
  ('network_type', 'vital',   '活力长居', 'vital',   'park', 1, 1, '业态（NetworkType.VITAL）',   NOW(), NOW(), 'system', 'system', 0),
  ('network_type', 'care',    '照护长居', 'care',    'park', 2, 1, '业态（NetworkType.CARE）',    NOW(), NOW(), 'system', 'system', 0),
  ('network_type', 'sojourn', '旅游短居',     'sojourn', 'park', 3, 1, '业态（NetworkType.SOJOURN）', NOW(), NOW(), 'system', 'system', 0)
ON DUPLICATE KEY UPDATE `updated_at` = `updated_at`;
