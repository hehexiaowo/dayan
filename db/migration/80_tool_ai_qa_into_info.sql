SET NAMES utf8mb4;
-- =====================================================================
-- 80_tool_ai_qa_into_info.sql  你问我答人物并入 tool_info
--
-- 人物配置不再走 tool_ai_qa_config 表：一个人物 = tool_info 一条
-- tool_type='ai_qa' 的实例，人物属性（头像/颜色/开场白/推荐问题/
-- 知识库/人设）放 config_json，由端上/聊天链路按 toolCode 消费。
--
-- 1) 存量人物搬移：tool_ai_qa_config → tool_info（防御性迁移，
--    全新初始化无数据时零行；tool_code 用高位偏移 TL9xxxx 防与应用
--    序列 code:seq:TL:0 冲突）
-- 2) 会话回填 tool_code 后 DROP 人物表；会话表删 config_id/config_code
--    （persona_name 冗余保留，会话展示不变）
-- 3) 预置 TL00004 补默认人设/开场白（admin 可再改）
-- 4) tool:qa:* 按钮权限下线（定义 + 角色绑定，同 44 迁移先例）
-- =====================================================================

-- ---------------------------------------------------------------------
-- 1. 存量人物搬移 tool_ai_qa_config → tool_info
-- ---------------------------------------------------------------------
INSERT INTO `tool_info`
  (`tool_code`, `tool_name`, `tool_type`, `tool_desc`, `config_json`,
   `status`, `remark`, `created_at`, `updated_at`, `creator`, `updater`, `deleted`)
SELECT
  CONCAT('TL', LPAD(90000 + c.`id`, 5, '0')),
  c.`persona_name`, 'ai_qa', CONCAT('AI 问答人物：', c.`persona_name`),
  JSON_OBJECT(
    'icon', c.`icon`,
    'iconColor', c.`icon_color`,
    'welcomeMsg', c.`welcome_msg`,
    'recommendQuestions', c.`recommend_questions`,
    'repoIds', c.`repo_ids`,
    'systemPrompt', c.`system_prompt`
  ),
  c.`status`, c.`remark`, NOW(), NOW(), 'system', 'system', 0
FROM `tool_ai_qa_config` c
WHERE NOT EXISTS (SELECT 1 FROM `tool_info` t WHERE t.`tool_code` = CONCAT('TL', LPAD(90000 + c.`id`, 5, '0')));

-- ---------------------------------------------------------------------
-- 2. 会话回填 tool_code（按 config_id 关联新实例；旧会话 tool_code 均为
--    76 迁移默认值 TL00004，全部按人物映射回填）
-- ---------------------------------------------------------------------
UPDATE `tool_ai_qa_session` s
JOIN `tool_ai_qa_config` c ON c.`id` = s.`config_id`
SET s.`tool_code` = CONCAT('TL', LPAD(90000 + c.`id`, 5, '0'));

-- ---------------------------------------------------------------------
-- 3. 预置 TL00004「你问我答」补默认人设/开场白（通用答疑，admin 可改）
-- ---------------------------------------------------------------------
UPDATE `tool_info`
   SET `config_json` = JSON_OBJECT(
         'iconColor', 'red',
         'welcomeMsg', '您好，我是您的人工智能展业助手，有什么可以帮您？',
         'systemPrompt', '你是一名专业的保险行业展业助手，服务对象是保险代理人。回答要专业、准确、简洁，用简体中文；不确定的内容如实说明，不编造。'
       ),
       `updated_at` = NOW()
 WHERE `tool_code` = 'TL00004';

-- ---------------------------------------------------------------------
-- 4. 会话表删人物外键冗余列（persona_name 保留）
-- ---------------------------------------------------------------------
ALTER TABLE `tool_ai_qa_session`
  DROP COLUMN `config_id`,
  DROP COLUMN `config_code`;

-- ---------------------------------------------------------------------
-- 5. DROP 人物配置表（数据已搬移）
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `tool_ai_qa_config`;

-- ---------------------------------------------------------------------
-- 6. tool:qa:* 按钮权限下线（定义 + 角色绑定，同 44 迁移先例）
-- ---------------------------------------------------------------------
DELETE FROM `organ_role_permission_ship` WHERE `permission_code` LIKE 'tool:qa:%';
DELETE FROM `organ_permission` WHERE `permission_code` LIKE 'tool:qa:%';
