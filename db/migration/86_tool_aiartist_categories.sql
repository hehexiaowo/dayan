SET NAMES utf8mb4;
-- =====================================================================
-- 86_tool_aiartist_categories.sql  AI 创作分类化
--
-- AI 创作参照你问我答模式：按功能分类成多个 aiartist 实例，
-- 分类的创作目的与提示词配置写入各实例 config_json（共用一套流水线）。
-- 1) TL00003 改名「AI创作（主题创作）」（内容转写，purpose=science；
--    保留编码——历史草稿 tool_code 引用）
-- 2) 新增 TL90006「AI创作（机构介绍）」（purpose=park）
-- 3) 新增 TL90007「AI创作（保险计划）」（purpose=product）
-- =====================================================================

UPDATE `tool_info`
   SET `tool_name` = 'AI创作（主题创作）',
       `tool_desc` = '选择、上传或粘贴文章，进行内容转写与再创作',
       `config_json` = JSON_OBJECT(
         'purpose', 'science',
         'icon', '主',
         'iconColor', 'blue',
         'systemPrompt', '你是大雁养老的资深内容编辑，擅长把给定文章转写为结构清晰、通俗易懂、有传播力的保险行业科普内容。转写须忠于原文事实，不编造资料外信息；用简体中文，先给读者最关心的结论，再分层展开；结尾给出认知引导而非硬推销。'
       ),
       `updated_at` = NOW()
 WHERE `tool_code` = 'TL00003';

INSERT INTO `tool_info`
  (`tool_code`, `tool_name`, `tool_type`, `tool_desc`, `config_json`,
   `status`, `remark`, `created_at`, `updated_at`, `creator`, `updater`, `deleted`)
VALUES
  ('TL90006', 'AI创作（机构介绍）', 'aiartist', '选择某个养老机构，进行机构的介绍与亮点总结',
   JSON_OBJECT(
     'purpose', 'park',
     'icon', '机',
     'iconColor', 'green',
     'systemPrompt', '你是大雁养老的机构推荐顾问，擅长把养老机构资料整理成客观、有温度的介绍。机构事实（床位、房型、服务、价格）必须 100% 来自素材，价格统一以机构最新报价为准；用简体中文，先概述机构定位，再按环境、照护服务、费用说明分层介绍；结尾给客户留出咨询引导。'
   ),
   1, '预置：AI 创作分类（机构介绍）',
   NOW(), NOW(), 'system', 'system', 0),
  ('TL90007', 'AI创作（保险计划）', 'aiartist', '上传已有的保险计划书，进行计划书的重新组织与表达丰富',
   JSON_OBJECT(
     'purpose', 'product',
     'icon', '保',
     'iconColor', 'orange',
     'systemPrompt', '你是大雁养老的保险方案专家，擅长把已有的保险计划书重新组织表达：保留条款与数据原意，把专业术语转成客户听得懂的话，突出保障利益与养老权益的结合点。条款、费率、保额等数据只取自计划书与知识库资料；用简体中文，先讲方案解决什么问题，再拆解保障责任，结尾给出明确的下一步咨询动作。'
   ),
   1, '预置：AI 创作分类（保险计划）',
   NOW(), NOW(), 'system', 'system', 0)
ON DUPLICATE KEY UPDATE
  `tool_name` = VALUES(`tool_name`),
  `tool_desc` = VALUES(`tool_desc`),
  `config_json` = VALUES(`config_json`),
  `updated_at` = NOW();
