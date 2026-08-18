SET NAMES utf8mb4;
-- =====================================================================
-- 81_tool_domain_rename.sql  工具域表名缩写 + tool_type 枚举值收敛
--
-- 缩写规则：ai_creator→aicrt / ai_qa→aichat / gap_calculator→gapcal /
--           pension_calculator→perncal（表名、类名、枚举值统一）
-- 历史迁移（70/74/75/76/80）不回改，全新初始化由本文件收敛到终态。
-- =====================================================================

-- ---------------------------------------------------------------------
-- 1. 五张记录表 RENAME（数据全量保留）
-- ---------------------------------------------------------------------
RENAME TABLE `tool_ai_creator_record`     TO `tool_aicrt_record`;
RENAME TABLE `tool_ai_qa_session`          TO `tool_aichat_session`;
RENAME TABLE `tool_ai_qa_message`          TO `tool_aichat_message`;
RENAME TABLE `tool_gap_calculator_record`  TO `tool_gapcal_record`;
RENAME TABLE `tool_pension_calculator_record` TO `tool_perncal_record`;

ALTER TABLE `tool_aicrt_record`    COMMENT = 'AI 创作使用记录（按 tool_code 区分公众号/小红书等实例）';
ALTER TABLE `tool_aichat_session`  COMMENT = 'AI 问答会话（按代理人归属，persona 冗余自 tool_info.tool_name）';
ALTER TABLE `tool_aichat_message`  COMMENT = 'AI 问答消息';
ALTER TABLE `tool_gapcal_record`   COMMENT = '养老缺口计算器使用记录';
ALTER TABLE `tool_perncal_record`  COMMENT = '社保养老计算器使用记录';

-- ---------------------------------------------------------------------
-- 2. 会话表索引修复：idx_agent_persona 引用已删列 config_id（80 遗留），
--    重建为 idx_agent_tool(agent_code, tool_code)
-- ---------------------------------------------------------------------
ALTER TABLE `tool_aichat_session`
  DROP INDEX `idx_agent_persona`;

ALTER TABLE `tool_aichat_session`
  ADD KEY `idx_agent_tool` (`agent_code`, `tool_code`);

-- ---------------------------------------------------------------------
-- 3. tool_info.tool_type 枚举值收敛（ai_qa→aichat / ai_creator→aicrt）
-- ---------------------------------------------------------------------
UPDATE `tool_info` SET `tool_type` = 'aicrt'  WHERE `tool_type` = 'ai_creator';
UPDATE `tool_info` SET `tool_type` = 'aichat' WHERE `tool_type` = 'ai_qa';
