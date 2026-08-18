SET NAMES utf8mb4;
-- =====================================================================
-- 74_tool_ai_creator.sql  AI 创作迁入 tool 域
-- 1) ai_creation_project → tool_ai_creator（RENAME 保留全部数据与索引；
--    列不变，新增 materials 快照列——前端供材，digest 阶段一次性消费）
-- 2) tool_info 种子 TL00003「AI 创作」（工具获客列表入口；获客宫格入口在前端）
-- 注意：RENAME 非幂等，手动执行一次（本库迁移按惯例手工应用）。
-- =====================================================================

RENAME TABLE `ai_creation_project` TO `tool_ai_creator`;

ALTER TABLE `tool_ai_creator`
  ADD COLUMN `materials` LONGTEXT NULL COMMENT '素材快照 JSON（前端供材 {type,title,text}[]，digest 阶段消费）' AFTER `material_refs`;

ALTER TABLE `tool_ai_creator` COMMENT = 'AI 创作项目（tool 域六阶段流水线）';

INSERT INTO `tool_info`
  (`tool_code`, `tool_name`, `tool_type`, `tool_desc`, `icon`, `entry_path`, `config`,
   `visible_scope`, `sort_order`, `status`, `remark`,
   `created_at`, `updated_at`, `creator`, `updater`, `deleted`)
VALUES
  ('TL00003', 'AI 创作', 4, '六阶段 AI 图文创作：素材→策略→大纲→正文→配图→成品',
   'AI', '/pages/acquisition/tools/ai-create/index', JSON_OBJECT('color', 'red'),
   'agent', 3, 1, '预置：agent 端获客工具',
   NOW(), NOW(), 'system', 'system', 0)
ON DUPLICATE KEY UPDATE `updated_at` = `updated_at`;
