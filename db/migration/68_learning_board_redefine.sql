SET NAMES utf8mb4;
-- =====================================================================
-- 68_learning_board_redefine.sql  学习中心板块化：category 语义重定义
--
-- 背景：agent 学习中心重组为四大板块（大雁课程/渠道课程/外部课程/雁鸣中国），
--   admin「课程管理」同步按板块做 tab 管理。大雁课程 = course_info（平台自研，
--   不在本表）；其余三板块 = learning_content.category。
--
-- 变更（纯语义重定义，存量 12 行数据不动、无需新枚举值）：
--   category 1：视频课程 → 渠道课程（存量 4 条渠道培训视频自然归入）
--   category 2：图文课程 → 外部课程（存量 4 条外部引进图文自然归入）
--   category 3：雁鸣中国（不变）
--
-- 同步：
--   1. system_dict learning_category 三个 dict_name 更名；
--   2. learning_content.category 列注释更新；
--   3. organ_permission 补 learning:content:*（admin 课程管理页板块 tab CRUD，
--      Controller = dayan-module-agent controller/admin/LearningContentAdminController）。
-- =====================================================================

-- ---------- 1. 字典更名 ----------
UPDATE `system_dict`
   SET `dict_name` = '渠道课程', `updated_at` = NOW()
 WHERE `dict_type` = 'learning_category' AND `dict_code` = '1' AND `deleted` = 0;
UPDATE `system_dict`
   SET `dict_name` = '外部课程', `updated_at` = NOW()
 WHERE `dict_type` = 'learning_category' AND `dict_code` = '2' AND `deleted` = 0;
UPDATE `system_dict`
   SET `dict_name` = '雁鸣中国', `updated_at` = NOW()
 WHERE `dict_type` = 'learning_category' AND `dict_code` = '3' AND `deleted` = 0;

-- ---------- 2. 列注释对齐 ----------
ALTER TABLE `learning_content`
  MODIFY COLUMN `category` TINYINT NOT NULL COMMENT '板块分类（1=渠道课程 2=外部课程 3=雁鸣中国）';

-- ---------- 3. admin 接口权限（课程管理页板块 tab 用） ----------
INSERT INTO `organ_permission`
  (`permission_code`, `permission_name`, `parent_code`, `permission_type`,
   `path`, `method`,
   `sort_order`, `status`, `remark`,
   `created_at`, `updated_at`, `creator`, `updater`, `deleted`)
VALUES
  ('learning:content:list',   '学习内容列表', 'learning:content', 3, '/admin-api/learning-contents',        'GET',    220, 1, '学习中心板块内容（渠道/外部/雁鸣）', NOW(), NOW(), 'system', 'system', 0),
  ('learning:content:query',  '学习内容详情', 'learning:content', 3, '/admin-api/learning-contents/*',      'GET',    221, 1, '学习中心板块内容', NOW(), NOW(), 'system', 'system', 0),
  ('learning:content:create', '新增学习内容', 'learning:content', 3, '/admin-api/learning-contents',        'POST',   222, 1, '学习中心板块内容', NOW(), NOW(), 'system', 'system', 0),
  ('learning:content:update', '修改学习内容', 'learning:content', 3, '/admin-api/learning-contents/*',      'PUT',    223, 1, '学习中心板块内容', NOW(), NOW(), 'system', 'system', 0),
  ('learning:content:delete', '删除学习内容', 'learning:content', 3, '/admin-api/learning-contents/*',      'DELETE', 224, 1, '学习中心板块内容', NOW(), NOW(), 'system', 'system', 0)
ON DUPLICATE KEY UPDATE `updated_at` = `updated_at`;
