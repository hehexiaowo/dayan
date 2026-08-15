SET NAMES utf8mb4;
-- =====================================================================
-- 64_agent_content.sql  代理人 AI 生成个人内容
--
-- 设计要点：
-- 1. 只存生成结果快照：素材引用（范文/知识库文档/商品）以 JSON 快照冗余，
--    素材侧后续变更不影响历史内容展示；
-- 2. content_body 按形态存储：1=图文（HTML 片段）/ 2=朋友圈（纯文本）/
--    3=视频脚本（结构化文本）；
-- 3. 含 channel_code → 走 MyBatis-Plus 租户自动隔离，业务层再按 agent_code
--    精确过滤（防跨人越权，agentCode 来自登录上下文，前端不传）。
-- =====================================================================
CREATE TABLE IF NOT EXISTS `agent_content` (
  `id`              BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `agent_code`      VARCHAR(64) NOT NULL COMMENT '代理人编码',
  `channel_code`    VARCHAR(50) NOT NULL COMMENT '渠道编码',
  `title`           VARCHAR(200) NOT NULL COMMENT '标题',
  `summary`         VARCHAR(500) DEFAULT NULL COMMENT '摘要',
  `cover_image`     VARCHAR(255) DEFAULT NULL COMMENT '封面（OSS key）',
  `content_type`    TINYINT NOT NULL COMMENT '形态（1=图文 2=朋友圈 3=视频脚本）',
  `content_body`    TEXT NOT NULL COMMENT '正文（图文=HTML；朋友圈=纯文本；脚本=结构化文本）',
  `style_code`      VARCHAR(32) DEFAULT NULL COMMENT '风格档位（professional/warm/authoritative/colloquial）',
  `ref_content_code` VARCHAR(64) DEFAULT NULL COMMENT '参考范文 contentCode',
  `ref_kb_files`    TEXT DEFAULT NULL COMMENT '勾选知识库文档 JSON（[{fileId,fileName}]）',
  `ref_goods_codes` TEXT DEFAULT NULL COMMENT '勾选商品 codes JSON（["GDxxx"]）',
  `status`          TINYINT NOT NULL DEFAULT 1 COMMENT '状态（1=正常）',
  `created_at`      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `creator`         VARCHAR(64) NOT NULL DEFAULT 'system' COMMENT '创建人',
  `updater`         VARCHAR(64) NOT NULL DEFAULT 'system' COMMENT '更新人',
  `deleted`         TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除（0=未删,1=已删）',
  `deleted_at`      DATETIME DEFAULT NULL COMMENT '删除时间',
  PRIMARY KEY (`id`),
  KEY `idx_agent_code` (`agent_code`),
  KEY `idx_channel_code` (`channel_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='代理人 AI 生成个人内容';
