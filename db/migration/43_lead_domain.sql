-- 强制连接字符集为 utf8mb4，避免 docker-entrypoint-initdb.d 按默认字符集读取
-- 导致中文双重编码（Mojibake）。必须作为第一条语句执行。
SET NAMES utf8mb4;
-- =====================================================================
-- 43_lead_domain.sql  独立 lead 域（访客线索）
--
-- 背景：原 agent_lead 把"匿名访客身份"与"代理人 CRM 线索"混在一张表，
--       agent_lead_trace 混合承载内容/工具/海报三类互动明细。
--       拆分为独立 lead 域：
--         lead_info                访客线索唯一身份（不绑定代理人，归属渠道；
--                                  openid/union_id/phone 渐进式补全；
--                                  留资/注册后回填 client_code 关联 client_info）
--         lead_content_read_record 内容阅读线索（含分享人 channel/agent）
--         lead_tool_use_record     工具使用线索（含分享人 channel/agent）
--         lead_poster_view_record  海报浏览线索（含分享人 channel/agent；
--                                  原 trackShare shareType=3 的对等承接，不丢历史语义）
--       agent_lead 降级为纯代理人 CRM：新增 visitor_lead_code 关联 lead_info，
--       访客不再自动写入 agent_lead（改为代理人从线索池认领）。
--
-- 内容：
--   1. 建 lead 域 4 张表（含 channel_code，参与渠道分片；雪花ID）
--   2. agent_lead 增加 visitor_lead_code 列
--   3. 存量数据迁移：agent_lead 中 visitor_token 非空的访客行 → lead_info
--      （lead_code 确定性生成 'VL'+原行雪花 id），并回写 visitor_lead_code；
--      agent_lead_trace 按 trace_type 拆入三张记录表（id 沿用，幂等可重跑）
--   4. 删除 agent_lead_trace 旧表
-- =====================================================================

-- ---------------------------------------------------------------------
-- 1.1 lead_info 访客线索（唯一身份，归属渠道）
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `lead_info` (
  `id` BIGINT NOT NULL COMMENT '主键（雪花ID）',
  `lead_code` VARCHAR(50) NOT NULL COMMENT '线索编码（VL+日期+序号，全平台唯一）',
  `visitor_token` VARCHAR(64) NOT NULL COMMENT '访客令牌（匿名唯一标识，UUID）',
  `channel_code` VARCHAR(50) NOT NULL COMMENT '所属渠道编码（首触渠道）',
  `openid` VARCHAR(100) DEFAULT NULL COMMENT '微信OpenID（授权后回填）',
  `union_id` VARCHAR(100) DEFAULT NULL COMMENT '微信UnionID（授权后回填）',
  `phone` VARCHAR(20) DEFAULT NULL COMMENT '手机号（留资后回填）',
  `name` VARCHAR(100) DEFAULT NULL COMMENT '姓名/称呼（留资后回填）',
  `wx_nickname` VARCHAR(100) DEFAULT NULL COMMENT '微信昵称',
  `wx_avatar` VARCHAR(500) DEFAULT NULL COMMENT '微信头像URL',
  `visitor_source` VARCHAR(20) DEFAULT NULL COMMENT '访客环境来源（wechat/browser/unknown）',
  `source_type` TINYINT(2) NOT NULL DEFAULT 4 COMMENT '来源类型（1=内容分享, 2=工具分享, 3=海报分享, 4=直接访问）',
  `source_code` VARCHAR(100) DEFAULT NULL COMMENT '来源编码（首个触点的 bizCode）',
  `client_code` VARCHAR(50) DEFAULT NULL COMMENT '关联客户编码（留资/注册后回填 client_info.client_code）',
  `last_interact_time` DATETIME DEFAULT NULL COMMENT '最后互动时间',
  `last_interact_type` TINYINT(2) DEFAULT NULL COMMENT '最后互动类型（1=内容 2=工具 3=海报）',
  `interact_count` INT(11) NOT NULL DEFAULT 0 COMMENT '互动总次数',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `creator` VARCHAR(64) DEFAULT 'system' COMMENT '创建人',
  `updater` VARCHAR(64) DEFAULT 'system' COMMENT '更新人',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：1已删除/0未删除',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '删除时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_visitor_token` (`visitor_token`),
  UNIQUE KEY `uk_lead_code` (`lead_code`),
  KEY `idx_channel_code` (`channel_code`),
  KEY `idx_phone` (`phone`),
  KEY `idx_client_code` (`client_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='访客线索';

-- ---------------------------------------------------------------------
-- 1.2 lead_content_read_record 内容阅读线索
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `lead_content_read_record` (
  `id` BIGINT NOT NULL COMMENT '主键（雪花ID）',
  `lead_code` VARCHAR(50) NOT NULL COMMENT '线索编码（lead_info.lead_code）',
  `visitor_token` VARCHAR(64) NOT NULL COMMENT '访客令牌（冗余，便于直查）',
  `channel_code` VARCHAR(50) NOT NULL COMMENT '渠道编码（分享人所属渠道）',
  `agent_code` VARCHAR(50) DEFAULT NULL COMMENT '分享人代理人编码（NULL=直接访问）',
  `content_code` VARCHAR(50) NOT NULL COMMENT '内容编码',
  `content_title` VARCHAR(200) DEFAULT NULL COMMENT '内容标题（冗余快照）',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '阅读时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `creator` VARCHAR(64) DEFAULT 'system' COMMENT '创建人',
  `updater` VARCHAR(64) DEFAULT 'system' COMMENT '更新人',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：1已删除/0未删除',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '删除时间',
  PRIMARY KEY (`id`),
  KEY `idx_lead_code` (`lead_code`),
  KEY `idx_visitor_token` (`visitor_token`),
  KEY `idx_agent_code` (`agent_code`),
  KEY `idx_content_code` (`content_code`),
  KEY `idx_channel_code` (`channel_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='内容阅读线索记录';

-- ---------------------------------------------------------------------
-- 1.3 lead_tool_use_record 工具使用线索
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `lead_tool_use_record` (
  `id` BIGINT NOT NULL COMMENT '主键（雪花ID）',
  `lead_code` VARCHAR(50) NOT NULL COMMENT '线索编码（lead_info.lead_code）',
  `visitor_token` VARCHAR(64) NOT NULL COMMENT '访客令牌（冗余，便于直查）',
  `channel_code` VARCHAR(50) NOT NULL COMMENT '渠道编码（分享人所属渠道）',
  `agent_code` VARCHAR(50) DEFAULT NULL COMMENT '分享人代理人编码（NULL=直接访问）',
  `tool_code` VARCHAR(100) NOT NULL COMMENT '工具编码（tool_info.tool_code；历史数据可能为路径标识）',
  `tool_name` VARCHAR(200) DEFAULT NULL COMMENT '工具名称（冗余快照）',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '使用时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `creator` VARCHAR(64) DEFAULT 'system' COMMENT '创建人',
  `updater` VARCHAR(64) DEFAULT 'system' COMMENT '更新人',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：1已删除/0未删除',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '删除时间',
  PRIMARY KEY (`id`),
  KEY `idx_lead_code` (`lead_code`),
  KEY `idx_visitor_token` (`visitor_token`),
  KEY `idx_agent_code` (`agent_code`),
  KEY `idx_tool_code` (`tool_code`),
  KEY `idx_channel_code` (`channel_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工具使用线索记录';

-- ---------------------------------------------------------------------
-- 1.4 lead_poster_view_record 海报浏览线索
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `lead_poster_view_record` (
  `id` BIGINT NOT NULL COMMENT '主键（雪花ID）',
  `lead_code` VARCHAR(50) NOT NULL COMMENT '线索编码（lead_info.lead_code）',
  `visitor_token` VARCHAR(64) NOT NULL COMMENT '访客令牌（冗余，便于直查）',
  `channel_code` VARCHAR(50) NOT NULL COMMENT '渠道编码（分享人所属渠道）',
  `agent_code` VARCHAR(50) DEFAULT NULL COMMENT '分享人代理人编码（NULL=直接访问）',
  `template_code` VARCHAR(50) NOT NULL COMMENT '海报模板编码',
  `poster_title` VARCHAR(200) DEFAULT NULL COMMENT '海报标题（冗余快照）',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '浏览时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `creator` VARCHAR(64) DEFAULT 'system' COMMENT '创建人',
  `updater` VARCHAR(64) DEFAULT 'system' COMMENT '更新人',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：1已删除/0未删除',
  `deleted_at` DATETIME DEFAULT NULL COMMENT '删除时间',
  PRIMARY KEY (`id`),
  KEY `idx_lead_code` (`lead_code`),
  KEY `idx_visitor_token` (`visitor_token`),
  KEY `idx_agent_code` (`agent_code`),
  KEY `idx_template_code` (`template_code`),
  KEY `idx_channel_code` (`channel_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='海报浏览线索记录';

-- ---------------------------------------------------------------------
-- 2. agent_lead 增加 visitor_lead_code（降级为代理人 CRM）
-- ---------------------------------------------------------------------
ALTER TABLE `agent_lead`
  ADD COLUMN `visitor_lead_code` VARCHAR(50) DEFAULT NULL COMMENT '关联访客线索编码（lead_info.lead_code；手工录入为 NULL）' AFTER `source_ref`,
  ADD KEY `idx_visitor_lead_code` (`visitor_lead_code`);

-- ---------------------------------------------------------------------
-- 3. 存量数据迁移
-- ---------------------------------------------------------------------
-- 3.1 agent_lead 访客行 → lead_info
--     lead_code = 'VL' + 原行雪花 id：确定性且全局唯一（LD 号段仅渠道内唯一，不能直接复用）
INSERT INTO `lead_info`
  (`id`, `lead_code`, `visitor_token`, `channel_code`, `phone`, `name`,
   `wx_nickname`, `wx_avatar`, `visitor_source`, `source_type`, `source_code`,
   `last_interact_time`, `last_interact_type`, `interact_count`,
   `created_at`, `updated_at`, `creator`, `updater`, `deleted`, `deleted_at`)
SELECT
  l.id, CONCAT('VL', l.id), l.visitor_token, l.channel_code,
  l.phone, NULLIF(l.name, '匿名访客'),
  l.wx_nickname, l.wx_avatar, l.visitor_source,
  COALESCE(l.last_trace_type, 4), l.source_ref,
  l.last_trace_time, l.last_trace_type, COALESCE(l.trace_count, 0),
  l.created_at, l.updated_at, l.creator, l.updater, l.deleted, l.deleted_at
FROM `agent_lead` l
WHERE l.visitor_token IS NOT NULL AND l.visitor_token <> ''
  AND NOT EXISTS (SELECT 1 FROM `lead_info` li WHERE li.visitor_token = l.visitor_token);

-- 3.2 回写 agent_lead.visitor_lead_code
UPDATE `agent_lead` l
JOIN `lead_info` li ON li.visitor_token = l.visitor_token
SET l.visitor_lead_code = li.lead_code
WHERE l.visitor_lead_code IS NULL
  AND l.visitor_token IS NOT NULL AND l.visitor_token <> '';

-- 3.3 agent_lead_trace 按 trace_type 拆入三张记录表（id 沿用，幂等）
INSERT INTO `lead_content_read_record`
  (`id`, `lead_code`, `visitor_token`, `channel_code`, `agent_code`, `content_code`, `content_title`,
   `created_at`, `updated_at`, `creator`, `updater`, `deleted`)
SELECT
  t.id, li.lead_code, li.visitor_token, t.channel_code, t.agent_code, t.biz_code, t.biz_title,
  t.trace_time, t.trace_time, 'system', 'system', 0
FROM `agent_lead_trace` t
JOIN `agent_lead` l ON l.id = t.lead_id
JOIN `lead_info` li ON li.visitor_token = l.visitor_token
WHERE t.trace_type = 1 AND t.deleted = 0
  AND NOT EXISTS (SELECT 1 FROM `lead_content_read_record` r WHERE r.id = t.id);

INSERT INTO `lead_tool_use_record`
  (`id`, `lead_code`, `visitor_token`, `channel_code`, `agent_code`, `tool_code`, `tool_name`,
   `created_at`, `updated_at`, `creator`, `updater`, `deleted`)
SELECT
  t.id, li.lead_code, li.visitor_token, t.channel_code, t.agent_code, t.biz_code, t.biz_title,
  t.trace_time, t.trace_time, 'system', 'system', 0
FROM `agent_lead_trace` t
JOIN `agent_lead` l ON l.id = t.lead_id
JOIN `lead_info` li ON li.visitor_token = l.visitor_token
WHERE t.trace_type = 2 AND t.deleted = 0
  AND NOT EXISTS (SELECT 1 FROM `lead_tool_use_record` r WHERE r.id = t.id);

INSERT INTO `lead_poster_view_record`
  (`id`, `lead_code`, `visitor_token`, `channel_code`, `agent_code`, `template_code`, `poster_title`,
   `created_at`, `updated_at`, `creator`, `updater`, `deleted`)
SELECT
  t.id, li.lead_code, li.visitor_token, t.channel_code, t.agent_code, t.biz_code, t.biz_title,
  t.trace_time, t.trace_time, 'system', 'system', 0
FROM `agent_lead_trace` t
JOIN `agent_lead` l ON l.id = t.lead_id
JOIN `lead_info` li ON li.visitor_token = l.visitor_token
WHERE t.trace_type = 3 AND t.deleted = 0
  AND NOT EXISTS (SELECT 1 FROM `lead_poster_view_record` r WHERE r.id = t.id);

-- ---------------------------------------------------------------------
-- 4. 删除旧互动明细表
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS `agent_lead_trace`;
