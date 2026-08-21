SET NAMES utf8mb4;
-- =====================================================================
-- 94_tool_article_rewrite.sql  AI 文章转写记录
--
-- 按阶段组织JSON字段，每个阶段独立存储，便于扩展和修改。
-- 状态机：CREATED → CONTENT_FETCHED → SUMMARY_DONE → REWRITTEN → AUDITED → IMAGED → READY → PUBLISHED
--
-- 注意：文章转写功能需要在渠道配置中启用才能在 Agent 端使用。
-- 配置方式（二选一）：
--   1. Admin 端 → 渠道管理 → 选择渠道 → 工具配置 → 勾选「AI创作（文章转写）」
--   2. 直接在 channel_config_tool 表中插入记录（见下方示例）
-- =====================================================================

CREATE TABLE IF NOT EXISTS `tool_article_rewrite_record` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `tool_code` VARCHAR(32) NOT NULL COMMENT '所属工具实例（tool_info.tool_code）',
  `agent_code` VARCHAR(64) NOT NULL COMMENT '创建代理人编码（登录上下文注入）',
  `channel_code` VARCHAR(64) NOT NULL DEFAULT '' COMMENT '渠道编码（租户隔离）',

  -- 状态管理
  `status` VARCHAR(32) NOT NULL DEFAULT 'CREATED' COMMENT '状态（CREATED/CONTENT_FETCHED/SUMMARY_DONE/REWRITTEN/AUDITED/IMAGED/READY/PUBLISHED）',

  -- 第一步：内容获取（JSON）
  `content_fetch` JSON DEFAULT NULL COMMENT '内容获取结果JSON（sourceType/sourceUrl/originalTitle/originalSource/originalPublishTime/originalContent/fetchTime/fetchStatus）',

  -- 第二步：内容总结与价值判断（JSON）
  `summary_analysis` JSON DEFAULT NULL COMMENT '总结与价值判断JSON（contentSummary/viralValue/relevance/rewritePlans/selectedPlanIds）',

  -- 第三步：文章转写（JSON）
  `rewrite_result` JSON DEFAULT NULL COMMENT '转写结果JSON（results[{planId/title/body/summary/keywords/channelAdaptation/wordCount}]/currentPlanId）',

  -- 第四步：内容审核（JSON）
  `audit_result` JSON DEFAULT NULL COMMENT '审核结果JSON（results[{planId/items[{dimension/item/originalText/description/severity/suggestion/fixedText/fixed}]/fixedContent}]/currentPlanId）',

  -- 第五步：文章配图（JSON）
  `image_result` JSON DEFAULT NULL COMMENT '配图结果JSON（results[{planId/mainImage/cbodyImages}]/currentPlanId）',

  -- 第六步：自查与发布（JSON）
  `publish_info` JSON DEFAULT NULL COMMENT '自查与发布信息JSON（results[{planId/selfCheck/publishChannel/publishUrl/publishStatus/publishTime}]/currentPlanId/lastSaveTime）',

  -- 公共字段
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `creator` VARCHAR(64) NOT NULL DEFAULT 'system' COMMENT '创建人',
  `updater` VARCHAR(64) NOT NULL DEFAULT 'system' COMMENT '更新人',
  `deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '软删标记',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '软删时间',
  PRIMARY KEY (`id`),
  KEY `idx_tool_code` (`tool_code`),
  KEY `idx_agent_code` (`agent_code`),
  KEY `idx_channel_code` (`channel_code`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI文章转写记录';

-- 预置文章转写的 tool_info 实例（幂等插入）
INSERT INTO `tool_info`
  (`tool_code`, `tool_name`, `tool_type`, `tool_desc`, `config_json`,
   `status`, `remark`,
   `created_at`, `updated_at`, `creator`, `updater`, `deleted`)
SELECT 'TL90008', 'AI创作（文章转写）', 'aiartist', '从外部文章链接引入，AI辅助转写为适合不同渠道发布的内容',
  '{"purpose":"rewrite","contentType":1,"pipeline":{"prompts":{}}}',
  1, '文章转写功能，支持URL解析、内容总结、多方案转写、降AI味审核等',
  NOW(), NOW(), 'system', 'system', 0
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `tool_info` WHERE `tool_code` = 'TL90008');

-- =====================================================================
-- 渠道配置示例（可选）
--
-- 如需为特定渠道启用文章转写功能，可执行以下 SQL：
-- 将 'YOUR_CHANNEL_CODE' 替换为实际的渠道编码
-- =====================================================================

-- 示例：为渠道启用文章转写功能（config_type=0 表示基础可见性）
-- INSERT INTO `channel_config_tool`
--   (`channel_code`, `tool_code`, `config_type`, `config_json`, `status`,
--    `created_at`, `updated_at`, `creator`, `updater`, `deleted`)
-- SELECT 'YOUR_CHANNEL_CODE', 'TL90008', 0, '{}', 1,
--   NOW(), NOW(), 'system', 'system', 0
-- FROM DUAL
-- WHERE NOT EXISTS (
--   SELECT 1 FROM `channel_config_tool`
--   WHERE `channel_code` = 'YOUR_CHANNEL_CODE'
--     AND `tool_code` = 'TL90008'
--     AND `config_type` = 0
-- );

-- 批量为所有已启用 AI 创作的渠道启用文章转写功能
-- INSERT INTO `channel_config_tool`
--   (`channel_code`, `tool_code`, `config_type`, `config_json`, `status`,
--    `created_at`, `updated_at`, `creator`, `updater`, `deleted`)
-- SELECT cct.`channel_code`, 'TL90008', 0, '{}', 1,
--   NOW(), NOW(), 'system', 'system', 0
-- FROM `channel_config_tool` cct
-- WHERE cct.`tool_code` IN ('TL00003', 'TL90006', 'TL90007')
--   AND cct.`config_type` = 0
--   AND cct.`status` = 1
--   AND cct.`deleted` = 0
--   AND NOT EXISTS (
--     SELECT 1 FROM `channel_config_tool` cct2
--     WHERE cct2.`channel_code` = cct.`channel_code`
--       AND cct2.`tool_code` = 'TL90008'
--       AND cct2.`config_type` = 0
--   )
-- GROUP BY cct.`channel_code`;
