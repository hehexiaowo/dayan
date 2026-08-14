-- 强制连接字符集为 utf8mb4，避免 docker-entrypoint-initdb.d 按默认字符集读取
-- 导致中文双重编码（Mojibake）。必须作为第一条语句执行。
SET NAMES utf8mb4;
-- =====================================================================
-- 41_content_category_dict.sql  内容分类并入业务字典
--
-- 背景：content_category 是典型的"分类即字典"场景，独立建表与业务字典
--       （system_dict_business）能力重复，收敛进字典体系统一管理。
--
-- 内容：
--   1. system_dict_business 补充 icon 与 extra(JSON) 两列，承接分类的
--      图标 / 封面图 / 是否可见 等扩展属性
--   2. 存量 content_category 数据迁入 system_dict_business
--      （dict_type='content_category', domain='content'；category_type 存 dict_value，
--       coverImage/isVisible 存 extra，description 存 remark；content_count 不再落库，
--       改由 content_info 实时统计）
--   3. 删除 content_category 表
--   4. 顺手补齐同类遗漏：learning_content.category 的三值枚举纳入业务字典
--      （dict_type='learning_category', domain='agent'），作为分类命名的唯一权威来源
-- =====================================================================

-- ---------------------------------------------------------------------
-- 1. system_dict_business 补列
-- ---------------------------------------------------------------------
ALTER TABLE `system_dict_business`
  ADD COLUMN `icon` VARCHAR(100) DEFAULT NULL COMMENT '图标（内容分类等场景使用）' AFTER `dict_value`,
  ADD COLUMN `extra` JSON DEFAULT NULL COMMENT '扩展属性（JSON）：内容分类的 coverImage/isVisible 等' AFTER `icon`;

-- ---------------------------------------------------------------------
-- 2. 存量内容分类迁入业务字典（幂等：已存在同 dict_type+dict_code 则跳过）
-- ---------------------------------------------------------------------
INSERT INTO `system_dict_business`
  (`dict_type`, `dict_code`, `dict_name`, `dict_value`, `icon`, `extra`,
   `parent_code`, `domain`, `sort_order`, `status`, `remark`,
   `created_at`, `updated_at`, `creator`, `updater`, `deleted`)
SELECT
  'content_category', c.category_code, c.category_name, CAST(c.category_type AS CHAR), c.icon,
  JSON_OBJECT('coverImage', c.cover_image, 'isVisible', c.is_visible),
  c.parent_code, 'content', c.sort_order, c.status, c.description,
  NOW(), NOW(), 'system', 'system', 0
FROM `content_category` c
WHERE c.deleted = 0
  AND NOT EXISTS (
    SELECT 1 FROM `system_dict_business` d
    WHERE d.dict_type = 'content_category' AND d.dict_code = c.category_code
  );

-- ---------------------------------------------------------------------
-- 3. 删除 content_category 旧表
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `content_category`;

-- ---------------------------------------------------------------------
-- 4. 学习中心分类枚举 → 业务字典（learning_content.category 的 1/2/3）
-- ---------------------------------------------------------------------
INSERT INTO `system_dict_business`
  (`dict_type`, `dict_code`, `dict_name`, `dict_value`, `domain`, `sort_order`, `status`, `remark`,
   `created_at`, `updated_at`, `creator`, `updater`, `deleted`)
VALUES
  ('learning_category', '1', '视频课程', '1', 'agent', 1, 1, '学习中心分类（learning_content.category）', NOW(), NOW(), 'system', 'system', 0),
  ('learning_category', '2', '图文课程', '2', 'agent', 2, 1, '学习中心分类（learning_content.category）', NOW(), NOW(), 'system', 'system', 0),
  ('learning_category', '3', '雁鸣中国', '3', 'agent', 3, 1, '学习中心分类（learning_content.category）', NOW(), NOW(), 'system', 'system', 0)
ON DUPLICATE KEY UPDATE `updated_at` = `updated_at`;
