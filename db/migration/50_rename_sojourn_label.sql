SET NAMES utf8mb4;
-- =====================================================================
-- 50_rename_sojourn_label.sql  「旅居」文案统一更名「旅游短居」（用户指示）
--
-- 代码/seed 侧已同步替换（92 文件）；本迁移修正存量库数据：
--   1. 业态字典 network_type（sojourn）显示名
--   2. 三端权限表的权限名（角色授权 UI 可见）
-- 幂等：REPLACE 无匹配不变化；编号迁移按目录惯例一次性执行。
-- =====================================================================

UPDATE `system_dict_business`
  SET `dict_name` = REPLACE(`dict_name`, '旅居', '旅游短居')
  WHERE `dict_type` = 'network_type';

UPDATE `organ_permission`
  SET `permission_name` = REPLACE(`permission_name`, '旅居', '旅游短居');

UPDATE `channel_permission`
  SET `permission_name` = REPLACE(`permission_name`, '旅居', '旅游短居');

UPDATE `supplier_permission`
  SET `permission_name` = REPLACE(`permission_name`, '旅居', '旅游短居');
