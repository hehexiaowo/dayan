SET NAMES utf8mb4;
-- =====================================================================
-- 88_tool_aiartist_knowledge_repo.sql  AI 创作分类绑定知识库
--
-- 知识库定位调整：不再是素材阶段的主体选择，而是正文生成前的自动检索补充
-- （校验+补充定位）。分类实例通过 config_json 根级 repoIds 绑定知识库仓库，
-- 流水线在正文生成前以策略+大纲为 query 自动检索，结果注入正文/审计素材。
-- 三个预置分类默认绑定「大雁养老-总公司-知识库」（id=1）。
-- =====================================================================

UPDATE `tool_info`
   SET `config_json` = JSON_SET(
         COALESCE(`config_json`, JSON_OBJECT()),
         '$.repoIds', JSON_ARRAY(1)
       ),
       `updated_at` = NOW()
 WHERE `tool_type` = 'aiartist'
   AND `tool_code` IN ('TL00003', 'TL90006', 'TL90007');
