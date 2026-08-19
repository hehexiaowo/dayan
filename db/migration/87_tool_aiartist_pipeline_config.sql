SET NAMES utf8mb4;
-- =====================================================================
-- 87_tool_aiartist_pipeline_config.sql  AI 创作分类流水线配置外置
--
-- 六阶段流水线的全部可调参数从 Java 常量外置到各分类实例的 config_json.pipeline：
-- 各阶段温度、合规禁语、素材上限、标题/篇幅限制、配图规格与超时、
-- 形态/风格/受众文案、阶段提示词覆盖。三个预置分类写入同一套默认值
-- （与后端 ToolAiartistPipelineConfig 内置默认一致），后续可在后台按分类独立调优。
-- 注意：MySQL 用户变量不保留 JSON 类型（@pipeline 会退化为字符串），
-- 故 JSON_OBJECT 中经 JSON_EXTRACT(@pipeline, '$') 还原为 JSON 文档再嵌入。
-- =====================================================================

SET @pipeline = JSON_OBJECT(
  'temps', JSON_OBJECT(
    'digest', 0.2, 'strategy', 0.7, 'titles', 0.7, 'outline', 0.5,
    'body', 0.6, 'audit', 0.2, 'polish', 0.5, 'revise', 0.3
  ),
  'bannedPhrases', JSON_ARRAY('保证收益', '稳赚', '包赚', '最高级', '国家级', '顶级', '100%', '百分百', '绝对', '秒杀', '史上'),
  'materialMax', 8000,
  'titleCountLimit', 5,
  'scoreRange', JSON_ARRAY(70, 99),
  'polishKeepRatio', 0.95,
  'imagePollTimeoutMs', 90000,
  'imageRetryAfterFailures', 2,
  'imageFallbackPrompt', 'Warm lifestyle photograph, elderly care concept related to: {promptZh}, single subject, shallow depth of field',
  'coverSizeDefault', '1024*1024',
  'coverSizeXhs', '1080*1440',
  'nodeSize', '1280*720',
  'titleLimits', JSON_OBJECT('1', 30, '2', 20, '3', 15, '4', 20),
  'lengthWindows', JSON_OBJECT('1', JSON_ARRAY(800, 2500), '2', JSON_ARRAY(30, 400), '3', JSON_ARRAY(400, 2500), '4', JSON_ARRAY(350, 1500)),
  'imageCountHints', JSON_OBJECT(
    '1', 'coverImage 1 张（1024*1024）+ 正文节点配图 3-4 张（1280*720）',
    '3', '仅规划 coverImage 1 张（1024*1024），所有 nodes 的 imageInsertion 必须为 null',
    '4', 'coverImage 1 张（1080*1440）+ 节点配位合计 2-4 张（1280*720）'
  ),
  'formInstructions', JSON_OBJECT(
    '1', '微信公众号精品图文（1200-1500 字，HTML 片段 <h2>/<p>，标题 ≤30 字）',
    '2', '朋友圈文案（≤200 字纯文本 + 1-2 emoji + 1 个 #话题标签，标题=首句钩子 ≤20 字）',
    '3', '短视频口播脚本（60-90 秒，【画面】【口播】【字幕】分镜，标题 ≤15 字）',
    '4', '小红书笔记（600-800 字，Emoji 列表 + #标签段，标题 ≤20 字）'
  ),
  'styleInstructions', JSON_OBJECT(
    'professional', '专业科普风格：用词严谨、逻辑清晰、多用数据与术语，面向对养老品质有要求的家庭决策者',
    'warm', '温情软文风格：以长辈/家庭的真实生活场景切入，情感细腻、语气温暖，引发共鸣',
    'authoritative', '权威报告风格：结论先行、分点论述、数据化表达，塑造平台专业可信形象',
    'colloquial', '口语化风格：短句、亲切、像朋友聊天'
  ),
  'audienceInstructions', JSON_OBJECT(
    'children', '为父母养老做决策的子女（30-50 岁）：理性、数据与家庭责任视角，专业可信赖',
    'elder', '老人本人（55-75 岁）：直白温暖、短句、从老人自身利益出发，避免术语',
    'general', '40-70 岁客户及其子女：通俗易懂'
  ),
  'prompts', JSON_OBJECT()
);

-- TL00003 主题创作
UPDATE `tool_info`
   SET `config_json` = JSON_OBJECT(
         'purpose', 'science',
         'icon', '主',
         'iconColor', 'blue',
         'systemPrompt', '你是大雁养老的资深内容编辑，擅长把给定文章转写为结构清晰、通俗易懂、有传播力的保险行业科普内容。转写须忠于原文事实，不编造资料外信息；用简体中文，先给读者最关心的结论，再分层展开；结尾给出认知引导而非硬推销。',
         'pipeline', JSON_EXTRACT(@pipeline, '$')
       ),
       `updated_at` = NOW()
 WHERE `tool_code` = 'TL00003';

-- TL90006 机构介绍
UPDATE `tool_info`
   SET `config_json` = JSON_OBJECT(
         'purpose', 'park',
         'icon', '机',
         'iconColor', 'green',
         'systemPrompt', '你是大雁养老的机构推荐顾问，擅长把养老机构资料整理成客观、有温度的介绍。机构事实（床位、房型、服务、价格）必须 100% 来自素材，价格统一以机构最新报价为准；用简体中文，先概述机构定位，再按环境、照护服务、费用说明分层介绍；结尾给客户留出咨询引导。',
         'pipeline', JSON_EXTRACT(@pipeline, '$')
       ),
       `updated_at` = NOW()
 WHERE `tool_code` = 'TL90006';

-- TL90007 保险计划
UPDATE `tool_info`
   SET `config_json` = JSON_OBJECT(
         'purpose', 'product',
         'icon', '保',
         'iconColor', 'orange',
         'systemPrompt', '你是大雁养老的保险方案专家，擅长把已有的保险计划书重新组织表达：保留条款与数据原意，把专业术语转成客户听得懂的话，突出保障利益与养老权益的结合点。条款、费率、保额等数据只取自计划书与知识库资料；用简体中文，先讲方案解决什么问题，再拆解保障责任，结尾给出明确的下一步咨询动作。',
         'pipeline', JSON_EXTRACT(@pipeline, '$')
       ),
       `updated_at` = NOW()
 WHERE `tool_code` = 'TL90007';
