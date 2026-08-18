SET NAMES utf8mb4;
-- =====================================================================
-- 83_tool_aiartist_rename.sql  aicrt → aiartist（AI 创作 → AI 艺术家）
--
-- 承接 81：tool_aicrt_record / tool_type='aicrt' 进一步收敛为
-- aiartist（语义更贴切：AI 图文创作助手）。
-- =====================================================================

RENAME TABLE `tool_aicrt_record` TO `tool_aiartist_record`;

ALTER TABLE `tool_aiartist_record` COMMENT = 'AI 创作使用记录（AI 艺术家流水线，按 tool_code 区分公众号/小红书等实例）';

UPDATE `tool_info` SET `tool_type` = 'aiartist' WHERE `tool_type` = 'aicrt';
