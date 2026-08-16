SET NAMES utf8mb4;
-- =====================================================================
-- 70_ai_creation_project.sql  AI 创作项目（六阶段交互式流水线）
--
-- 各阶段产物以 JSON 列持久化，支持草稿中断恢复；
-- agent_code 服务端登录上下文注入，channel_code 走租户拦截器。
-- 文生图配置 key 与 llm.api-key 共用 DashScope API-Key。
-- =====================================================================

CREATE TABLE IF NOT EXISTS `ai_creation_project` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `agent_code` VARCHAR(64) NOT NULL COMMENT '创建代理人编码（登录上下文）',
  `channel_code` VARCHAR(64) NOT NULL DEFAULT '' COMMENT '渠道编码（租户隔离）',
  `purpose` VARCHAR(32) NOT NULL COMMENT '文章目的（product=产品宣传/park=机构推荐/science=科普获客）',
  `content_type` TINYINT NOT NULL COMMENT '内容形态（1图文 2朋友圈 3视频脚本 4小红书）',
  `style_code` VARCHAR(32) DEFAULT NULL COMMENT '写作风格（professional/warm/authoritative/colloquial）',
  `audience` VARCHAR(32) DEFAULT 'general' COMMENT '目标读者（children/elder/general）',
  `topic` VARCHAR(500) DEFAULT NULL COMMENT '主题/切入话题',
  `material_refs` JSON DEFAULT NULL COMMENT '素材引用 JSON（refContentCode/kbFileIds/goodsCodes/parkCodes）',
  `status` VARCHAR(32) NOT NULL COMMENT '阶段（CREATED/DIGESTED/STRATEGY_CONFIRMED/OUTLINE_CONFIRMED/BODY_DONE/IMAGES_DONE/SAVED）',
  `fact_digest` JSON DEFAULT NULL COMMENT '素材消化 JSON（hardFacts/softPoints/missing）',
  `strategy` JSON DEFAULT NULL COMMENT '策略面板 JSON（含 coreExecutionPrompt）',
  `titles` JSON DEFAULT NULL COMMENT '候选标题数组 JSON（title/tag/viralScore/reasoning）',
  `selected_title` VARCHAR(200) DEFAULT NULL COMMENT '选定标题',
  `outline` JSON DEFAULT NULL COMMENT '大纲 JSON（coverImage + nodes[]，含配图位规划）',
  `body` LONGTEXT COMMENT '正文（图文=HTML 片段；含 [AI_IMAGE_*] 占位符）',
  `audit_log` JSON DEFAULT NULL COMMENT '审计修正记录 JSON（type/message）',
  `scores` JSON DEFAULT NULL COMMENT '五维打分 JSON',
  `images` JSON DEFAULT NULL COMMENT '配图结果 JSON（placeholder/size/prompt/fileKey/url/status/error）',
  `warnings` JSON DEFAULT NULL COMMENT '流程提示 JSON 数组',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `creator` VARCHAR(64) NOT NULL DEFAULT 'system' COMMENT '创建人',
  `updater` VARCHAR(64) NOT NULL DEFAULT 'system' COMMENT '更新人',
  `deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '软删标记',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '软删时间',
  PRIMARY KEY (`id`),
  KEY `idx_agent_code` (`agent_code`),
  KEY `idx_channel_code` (`channel_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI 创作项目（六阶段流水线）';

-- ---------- system_config：llm 文生图配置（key 与 llm.api-key 共用 DashScope API-Key）----------
-- 列清单与幂等风格照抄 63_knowledge_repo.sql（本表实际列为 config_name/description，无 remark）
INSERT INTO `system_config`
  (`config_group`, `config_key`, `config_value`, `value_type`, `env`, `scope`,
   `config_name`, `description`, `is_secret`, `is_runtime`, `sort_order`,
   `created_at`, `updated_at`, `creator`, `updater`, `deleted`)
VALUES
  ('llm', 'llm.image-model', 'qwen-image-plus', 'string', 'prod', 'global',
   'AI 配图模型名', 'AI 配图模型名（DashScope 文生图，与 llm.api-key 共用凭据）', 0, 1, 80,
   NOW(), NOW(), 'system', 'system', 0),
  ('llm', 'llm.image-api-base', 'https://dashscope.aliyuncs.com', 'string', 'prod', 'global',
   'DashScope 文生图 API 基地址', 'DashScope 文生图 API 基地址（文生图接口域名前缀）', 0, 1, 90,
   NOW(), NOW(), 'system', 'system', 0)
ON DUPLICATE KEY UPDATE `updated_at` = `updated_at`;
