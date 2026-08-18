-- 强制连接字符集为 utf8mb4，避免 docker-entrypoint-initdb.d 按默认字符集读取
-- 导致中文双重编码（Mojibake）。必须作为第一条语句执行。
SET NAMES utf8mb4;
-- =====================================================================
-- 78_course_info_merge_learning.sql  learning_content 并入 course_info
--
-- 背景：渠道课程/外部课程/雁鸣中国（learning_content）与平台自研课程
--   （course_info）本质都是课程内容，统一收口到 course_info，用
--   course_source 板块维度区隔，删除 learning_content 表。
--
-- 维度设计：
--   course_type（形态：1=线上录播 2=线上直播 3=线下课程 4=混合课程）
--     保留，仅平台自研课程（大雁课程）使用，其余板块可为 NULL；
--   course_source（板块：1=平台自研 2=渠道课程 3=外部课程 4=雁鸣中国资讯）
--     新增，四个学习中心板块的区隔维度。
--
-- 变更：
--   1. course_info 新增 6 列（course_source/author/duration_text/course_body/badge/publish_time）
--   2. learning_content 12 条存量数据迁入 course_info（course_code 保留 LC 原值，
--      status 1上架→2已上架 / 0下架→3已下架，course_type=NULL，is_free=1）
--   3. DROP learning_content（实体与接口在代码中同步移除）
--   4. 清理 system_dict.learning_category 字典与 organ_permission.learning:content:* 权限
-- =====================================================================

-- ---------- 1. course_info 扩展列 ----------
ALTER TABLE `course_info`
  MODIFY COLUMN `course_type` TINYINT(2) DEFAULT NULL COMMENT '课程类型（1=线上录播, 2=线上直播, 3=线下课程, 4=混合课程；非平台自研板块可空）',
  ADD COLUMN `course_source` TINYINT NOT NULL DEFAULT 1 COMMENT '板块来源（1=平台自研大雁 2=渠道课程 3=外部课程 4=雁鸣中国资讯）' AFTER `course_type`,
  ADD COLUMN `course_body` TEXT DEFAULT NULL COMMENT '正文（详情页长文，纯文本）' AFTER `course_description`,
  ADD COLUMN `author` VARCHAR(100) DEFAULT NULL COMMENT '作者/来源（渠道/外部/资讯用，平台课程走讲师）' AFTER `learning_objectives`,
  ADD COLUMN `duration_text` VARCHAR(20) DEFAULT NULL COMMENT '时长展示文本（如 28:30 / 约 15 分钟）' AFTER `author`,
  ADD COLUMN `badge` VARCHAR(20) DEFAULT NULL COMMENT '角标（热/新/要闻/人物/动态/洞察）' AFTER `course_body`,
  ADD COLUMN `publish_time` DATETIME DEFAULT NULL COMMENT '发布时间（资讯/内容用，课程走开课日期）' AFTER `badge`,
  ADD KEY `idx_course_source` (`course_source`);

-- ---------- 2. 存量数据迁入（learning_content → course_info，板块映射 category+1） ----------
INSERT INTO `course_info`
  (`id`, `course_code`, `course_name`, `course_source`, `course_type`, `category_code`,
   `course_description`, `course_body`, `author`, `duration_text`, `badge`, `publish_time`,
   `view_count`, `sort_order`, `course_status`, `total_class`, `total_duration`,
   `original_price`, `sale_price`, `is_free`, `is_recommend`,
   `created_at`, `updated_at`, `creator`, `updater`)
SELECT `id`, `content_code`, `title`, `category` + 1, NULL, NULL,
       `summary`, `body`, `author`, `duration`, `badge`, `publish_time`,
       `view_count`, `sort_order`, CASE `status` WHEN 1 THEN 2 ELSE 3 END,
       0, NULL, 0, 0, 1, 0,
       `created_at`, `updated_at`, `creator`, `updater`
FROM `learning_content`
WHERE `deleted` = 0;

-- ---------- 3. 删除 learning_content ----------
DROP TABLE IF EXISTS `learning_content`;

-- ---------- 4. 清理字典与权限 ----------
DELETE FROM `system_dict` WHERE `dict_type` = 'learning_category';
DELETE FROM `organ_permission` WHERE `permission_code` LIKE 'learning:content:%';
DELETE FROM `organ_role_permission_ship` WHERE `permission_code` LIKE 'learning:content:%';

-- ---------- 5. 表注释更新 ----------
ALTER TABLE `course_info`
  COMMENT = '课程信息（学习中心四板块：大雁/渠道/外部/雁鸣中国，course_source 区隔）';
